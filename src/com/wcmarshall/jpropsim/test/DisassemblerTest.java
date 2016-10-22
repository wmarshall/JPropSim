package com.wcmarshall.jpropsim.test;

import com.wcmarshall.jpropsim.disassembler.Disassembler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class DisassemblerTest {


    @Test
    public void testGenerateListing() throws IOException {
        Disassembler disasm = new Disassembler();
        System.out.println(disasm.generateListing(new File("interpreter.bin")));
    }

}
