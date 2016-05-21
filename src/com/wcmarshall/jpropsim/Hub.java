package com.wcmarshall.jpropsim;

public class Hub {

	private static final int NUM_COGS = 8;
	private static final int HUB_RAM_SIZE = 32768;
	private Cog[] cogs = new Cog[NUM_COGS];
	private byte[] hubram = new byte[HUB_RAM_SIZE];

	private int alignment = 0;

	public Hub() {
		for (int i = 0; i < cogs.length; i++) {
			cogs[i] = new Cog(this, i);
		}
	}

	private int readBytes(int base, int count) {
		int retval = 0;
		for (int i = 0; i < count; i++) {
			retval |= hubram[base + i] << 8 * i;
		}
		return retval;
	}

	public int getLong(int addr) {
		return readBytes(addr, 4);
	}

	public int getWord(int addr) {
		return readBytes(addr, 2);
	}

	public int getByte(int addr) {
		return readBytes(addr, 1);
	}

	public boolean isAligned(Cog cog) {
		// TODO Auto-generated method stub
		return this.alignment == cog.getId();
	}

}
