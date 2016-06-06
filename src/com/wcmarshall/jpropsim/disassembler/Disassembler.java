package com.wcmarshall.jpropsim.disassembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Disassembler {

	public static final String HEX_FMT = "  %08X";
	public static final String DEC_FMT = "%10d";

	private String addressFMT = HEX_FMT;
	private String instrFMT = HEX_FMT;

	public Disassembler() {
		// TODO Auto-generated constructor stub
	}

	public String disassemble(int instruction) {
		return new Instruction(instruction).toString();
	}

	public String generateListing(File fin) throws IOException {
		FileInputStream input = new FileInputStream(fin);
		byte[] data = new byte[(int) fin.length()];
		input.read(data);
		input.close();
		int[] pgm = new int[data.length / 4];
		for (int i=0; i<pgm.length; i++) {
			pgm[i] = (data[i*4] & 0xFF) | ((data[i*4+1] << 8) & 0xFF00) | ((data[i*4+2] << 16) & 0xFF0000) | (data[i*4+3] << 24);
		}
		return generateListing(pgm);
	}

	public String generateListing(int[] pgm) {
		String disasm = "****************************\n" +
				"*    P8X32A DISASSEMBLY    *\n" +
				"****************************\n\n";

		String formatter = String.format(" %s  %s  %%s\n", addressFMT, instrFMT);

		for (int i=0; i<pgm.length; i++) {
			disasm += String.format(formatter, i, pgm[i], disassemble(pgm[i]));
		}
		return disasm;
	}
}
