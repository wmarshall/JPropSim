package com.wcmarshall.jpropsim;

public class Hub {

    private static final int NUM_COGS = 8;
    private static final int HUB_RAM_SIZE = 32768;
    private Cog[] cogs = new Cog[NUM_COGS];
    private byte[] hubram = new byte[HUB_RAM_SIZE];

    private int alignment = 0;
	private int cnt = 0;
	private int ina = 0;

    public Hub() {
        for (int i = 0; i < cogs.length; i++) {
            cogs[i] = new Cog(this, i);
        }
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
        for (int i = 0; i < count; i++) {
            retval |= (0xFF & hubram[base + i]) << 8 * i;
        }
        return retval;
    }

	private void writeBytes(int base, int value, int count) {
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
			outa |= c.getLong(Cog.OUTA_ADDR);
		}
		return outa & getDira();
	}

    public boolean isAligned(Cog cog) {
        return this.alignment == cog.getID();
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
}
