package com.wcmarshall.jpropsim.test;

import com.wcmarshall.jpropsim.disassembler.Disassembler;

import java.io.File;
import java.io.IOException;

public class DisassemblerTest {
    public static void main(String[] args) {
        Disassembler disasm = new Disassembler();
        try {
            System.out.println(disasm.generateListing(new File("interpreter.bin")));
        } catch (IOException e) {
            System.out.println("Unable to locate binary file");
        }
    }
}
