package com.wcmarshall.jpropsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Hub {

	public static BreakpointManager bpm = new BreakpointManager();

	private static final int NUM_COGS = 8;
    private static final int HUB_RAM_SIZE = 32768;
	private static final int HUB_ROM_SIZE = 32768;
    private Cog[] cogs = new Cog[NUM_COGS];
    private byte[] hubram = new byte[HUB_RAM_SIZE];
	private byte[] hubrom = new byte[HUB_ROM_SIZE];

    private int alignment = 0;
	private int cnt = 0;
	private int ina = 0;

    public Hub() throws IOException {
        for (int i = 0; i < cogs.length; i++) {
            cogs[i] = new Cog(this, i);
        }

		// load ROM file (only used for char set and tables)
		FileInputStream input = new FileInputStream(new File("rom.bin"));
		input.read(hubrom);
		input.close();
		// load interpreter into proper starting position
		input = new FileInputStream(new File("interpreter.bin"));
		input.read(hubrom, 0xF004 - 0x8000, 0x10000 - 0xF004);
		input.close();
		// pretend we're the bootloader and start the interpreter in cog 0
		initCog((1 << 18) + (0x3C01 << 4));
    }

	public Hub(File binFile) throws IOException {
		this();
		FileInputStream input = new FileInputStream(binFile);
		input.read(hubram);
		input.close();
	}

	/**
	 * Initializes a COG
	 *
	 * @param arg parameter register. Must fit specifications outlined by COGINIT instruction
	 * @return COG ID of initialized COG or -1 if no COGs are available
     */
	public int initCog(int arg) {

		int par = ((arg >> 18) & 0b1111111_1111111) << 2;
		int start = ((arg >> 4) & 0b1111111_1111111) << 2;
		int cogid = arg & 0b111;
		boolean cognew = ((arg >> 3) & 1) == 1;

		if (cognew) {
			int i;
			for (i=0; i<NUM_COGS; i++) {
				if (!cogs[i].isRunning())
					break;
			}
			if (i == cogs.length) return -1;
			cogid = cogs[i].getID();
		}

		cogs[cogid].start(start, par);
		return cogid;
	}

	/**
	 *
	 * @param id cog to stop
	 * @return carry bit, is true if all cogs were running prior to COGSTOP
     */
	public boolean stopCog(int id) {
		boolean carry = true;
		for (Cog c : cogs) {
			carry = carry & c.isRunning();
		}

		id &= 0b111;
		cogs[id].stop();
		return carry;
	}

    private int readBytes(int base, int count) {

		int retval = 0;
		byte[] memory;

		base &= 0xFFFF;

		if (base < HUB_RAM_SIZE) {
			memory = hubram;
		} else {
			memory = hubrom;
			base -= HUB_RAM_SIZE;
		}

		for (int i = 0; i < count; i++) {
			retval |= (0xFF & memory[base + i]) << 8 * i;
		}

        return retval;
    }

	private void writeBytes(int base, int value, int count) {

		base &= 0xFFFF;

		if (base >= HUB_RAM_SIZE) return;

		for (int i = 0; i < count; i++) {
			hubram[base + i] = (byte) (value >> 8 * i);
		}
	}

    public int getLong(int addr) {
        return readBytes(addr & ~0b11, 4);
    }

    public int getWord(int addr) {
        return readBytes(addr & ~1, 2);
    }

    public int getByte(int addr) {
        return readBytes(addr, 1);
    }

	public void setLong(int addr, int value) {
		writeBytes(addr & ~0b11, value, 4);
	}

	public void setWord(int addr, int value) {
		writeBytes(addr & ~1, value, 2);
	}

	public void setByte(int addr, int value) {
		writeBytes(addr, value, 1);
	}

	public int getCnt() {
		return cnt;
	}

	public int getIna() {
		return ina & ~getDira();
	}

	public void setPinIn(int pin, boolean state) {
		if (pin > 31) return;

		if (state) {
			ina |= 1 << pin;
		} else {
			ina &= ~(1 << pin);
		}
	}

	public int getDira() {
		int dira = 0;
		for (Cog c : cogs) {
			dira |= c.getLong(Cog.DIRA_ADDR);
		}
		return dira;
	}

	public int getOuta() {
		int outa = 0;
		for (Cog c : cogs) {
			// for a cog to set a pin high it must also set it to output in it's own DIRA register
			outa |= c.getLong(Cog.OUTA_ADDR) & c.getLong(Cog.DIRA_ADDR);
		}
		return outa & getDira();
	}

	public Cog getCog(int cogid) {
		if (cogid < 0 || cogid > NUM_COGS) return null;
		return cogs[cogid];
	}

    public boolean isAligned(Cog cog) {
        return this.alignment == cog.getID();
    }

	public int getAlignment() {
		return alignment;
	}

    public void tick() {
        for (Cog c : cogs) {
            c.tick();
        }
        // update cnt
		cnt++;
		if ((cnt & 1) == 0) {
			// every other tick, shift alignment
			this.alignment = (this.alignment + 1) % NUM_COGS;
		}
    }

	public void run() {
		do {
			tick();
		}while (!bpm.breakNow(this));
	}
}
