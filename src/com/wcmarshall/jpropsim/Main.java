package com.wcmarshall.jpropsim;

import com.wcmarshall.jpropsim.disassembler.Disassembler;

import java.io.File;
import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		Disassembler disasm = new Disassembler();
		System.out.println(disasm.generateListing(new File("PropPWM.binary")));
	}

}
