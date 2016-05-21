package com.wcmarshall.jpropsim;

import com.wcmarshall.jpropsim.disassembler.Instruction;

public class Cog {

	private static final int PAR_ADDR = 0x1f0;

	private final Hub hub;

	private final int id;

	private boolean running = false, prog_loaded = false;
	private int hub_prog_addr = 0, prog_load_count = 0;;

	private int pc = 0;

	private int[] cogram = new int[512];

	private boolean zflag = false, cflag = false;

	public Cog(Hub hub, int id) {
		this.id = id;
		this.hub = hub;
	}

	public boolean getZFlag() {
		return zflag;
	}

	public void setZFlag(boolean zflag) {
		this.zflag = zflag;
	}

	public boolean getCFlag() {
		return cflag;
	}

	public void setCFlag(boolean cflag) {
		this.cflag = cflag;
	}

	public int getLong(int addr) {
		if (addr > 0x1ef) {
			// SPR code
		}
		return cogram[addr];
	}

	public void setLong(int addr, int value) {
		if (addr > 0x1ef) {
			// SPR code
		}
		cogram[addr] = value;
	}

	public void start(int hub_prog_addr, int arg) {
		for (int i = 0; i < 0x1ff; i++) {
			cogram[i] = 0;
		}
		this.running = true;
		this.prog_loaded = false;
		this.hub_prog_addr = hub_prog_addr;
		this.prog_load_count = 0;
		this.setCFlag(false);
		this.setZFlag(false);
		cogram[PAR_ADDR] = arg;
	}

	public void tick() {
		if (running) {
			if (prog_loaded) {
				new Instruction(cogram[pc]).execute(this);

			} else {
				if (hub.isAligned(this)) {
					cogram[prog_load_count] = hub.getLong(this.hub_prog_addr + 4 * prog_load_count);
					prog_load_count++;
					if (prog_load_count > 0x1ef) {
						prog_loaded = true;
					}

				}
			}

		}

	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

}
