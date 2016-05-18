package com.wcmarshall.jpropsim;

public class Hub {

	private static final int NUM_COGS = 8;
	private static final int HUB_RAM_SIZE = 32768;
	private Cog[] cogs = new Cog[NUM_COGS];
	private byte[] hubRam = new byte[HUB_RAM_SIZE];

	public Hub() {
		for (int i = 0; i < cogs.length; i++) {
			cogs[i] = new Cog(i);
		}
	}

	public byte readByte(int addr) {
		return hubRam[addr];
	}

}
