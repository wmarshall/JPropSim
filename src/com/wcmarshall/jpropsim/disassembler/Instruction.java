package com.wcmarshall.jpropsim.disassembler;

import com.wcmarshall.jpropsim.Cog;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Instruction {


    private static Predicate<Cog> waitNPredicate(int n) {
        return new Predicate<Cog>() {
            int count = n;

            @Override
            public boolean test(Cog c) {
                return ((count > 0) ? (count--) : count) == 0;
            }

        };
    }

    private static Predicate<Cog> isAligned = c -> c.isHubAligned();

    private static Predicate<Cog> IOPredicate() {
        return isAligned.and(waitNPredicate(8));
    }

    ;

    private static Predicate<Cog> waitPredicate() {
        return waitNPredicate(6);
    }

    ;

    public enum OpCode {
        ABS(0b101010), ABSNEG(0b101011), ADD(0b100000), ADDABS(0b100010), ADDS(0b110100), ADDSX(0b110110), ADDX(
                0b110010), AND(0b011000), ANDN(0b011001), CMPS(0b110000), CMPSUB(0b111000), CMPSX(0b110001), DJNZ(
                0b111001), HUBOP(0b000011, IOPredicate()), JMP(0b010111), MOV(0b101000), MOVD(0b010101), MOVI(
                0b010110), MOVS(0b010100), MUXC(0b011100), MUXNC(0b011101), MUXNZ(0b011111), MUXZ(
                0b011110), NEG(0b101001), NEGC(0b101100), NEGNC(0b101101), NEGNZ(
                0b101111), NEGZ(0b101110), RCL(0b001101), RCR(0b001100), RDBYTE(
                0b000000,
                IOPredicate()), RDLONG(0b000010, IOPredicate()), RDWORD(
                0b000001,
                IOPredicate()), REV(0b001111), ROL(0b001001), ROR(
                0b001000), SAR(0b001110), SHL(0b001011), SHR(
                0b001010), SUB(0b100001), SUBABS(
                0b100011), SUBS(
                0b110101), SUBSX(
                0b110111), SUBX(
                0b110011), SUMC(
                0b100100), SUMNC(
                0b100101), SUMNZ(
                0b100111), SUMZ(
                0b100110), TJNZ(
                0b111010), TJZ(
                0b111011), WAITCNT(
                0b111110,
                waitPredicate()), WAITPEQ(
                0b111100,
                waitPredicate()), WAITPNE(
                0b111101,
                waitPredicate()), WAITVID(
                0b111111), XOR(
                0b011011);

        private final int instr;

        private final BiConsumer<Cog, Instruction> exec_fn;

        private final Predicate<Cog> executable;

        private OpCode(int instr, Predicate<Cog> executable) {
            this.instr = instr;
            this.exec_fn = null;
            this.executable = executable;
        }

        private OpCode(int instr) {
            this(instr, waitNPredicate(4));
        }

        public int getInstr() {
            return this.instr;
        }

        public boolean isExecutable(Cog cog) {
            return executable.test(cog);
        }

    }

    private static final Predicate<Cog> ifZ = c -> c.getZFlag();
    private static final Predicate<Cog> ifC = c -> c.getCFlag();

    public enum Condition {

        IF_ALWAYS(0b1111, c -> true), IF_NEVER(0b0000, c -> false), IF_Z(0b1010, ifZ), IF_NZ(0b0101,
                ifZ.negate()), IF_C(0b1100, ifC), IF_NC(0b0011, ifC.negate()), IF_Z_AND_C(0b1000,
                ifZ.and(ifC)), IF_Z_OR_C(0b1110, ifZ.or(ifC)), IF_Z_EQ_C(0b1001,
                c -> c.getZFlag() == c.getCFlag()), IF_Z_NE_C(0b0110,
                c -> c.getZFlag() != c.getCFlag()), IF_NZ_AND_NC(0b0001,
                ifZ.negate().and(ifC.negate())), IF_NZ_OR_NC(0b0111,
                ifZ.negate().or(ifC.negate())), IF_Z_AND_NC(0b0010,
                ifZ.and(ifC.negate())), IF_Z_OR_NC(0b1011,
                ifZ.or(ifC.negate())), IF_NZ_AND_C(0b0100,
                ifZ.negate().and(ifC)), IF_NZ_OR_C(
                0b1101, ifZ.negate().or(ifC));

        private final int cond;

        private final Predicate<Cog> test;

        private Condition(int cond, Predicate<Cog> test) {
            this.cond = cond;
            this.test = test;
        }

        public int getCond() {
            return this.cond;
        }

        public boolean testCond(Cog c) {
            return test.test(c);
        }
    }

    private OpCode opcode;

    private boolean write_zero, write_carry, write_result, immediate;

    private Condition condition;

    private int destination, source;

    private int start_exec_clk = 0;

    private boolean executed = false;

    public Instruction(int encoded) {
        int instr = encoded >> (32 - 6);
        for (OpCode opcode : OpCode.values()) {
            if (instr == opcode.getInstr()) {
                this.opcode = opcode;
            }
        }
        // assert (this.instruction != null); Garbage registers can be null
        // instr
        int zrci = (encoded >> (32 - 6 - 4)) & 0b1111;
        this.write_zero = (zrci & 0b1000) != 0;
        this.write_carry = (zrci & 0b100) != 0;
        this.write_result = (zrci & 0b10) != 0;
        this.immediate = (zrci & 0b1) != 0;
        int cond = (encoded >> (32 - 6 - 4 - 4)) & 0b1111;
        for (Condition condition : Condition.values()) {
            if (cond == condition.getCond()) {
                this.condition = condition;
            }
        }
        this.destination = (encoded >> (32 - 6 - 4 - 4 - 9)) & 0b111111111;
        this.source = (encoded >> (32 - 6 - 4 - 4 - 9 - 9)) & 0b111111111;
    }

    public int getSourceValue(Cog cog) {
        if (this.immediate) {
            return destination;
        }
        return cog.getLong(this.destination);
    }

    private void writeZ(Cog cog, boolean value) {
        if (this.write_zero) {
            cog.setZFlag(value);
        }
    }

    private void writeC(Cog cog, boolean value) {
        if (this.write_carry) {
            cog.setCFlag(value);
        }
    }

    private void writeResult(Cog cog, int value) {
        if (this.write_result) {
            // lol dunno
        }
    }

    /**
     * Execution occurs in phases 1. Test Condition - 0 cycles 2. perform
     * operation (or do nothing) - 4-23 cycles 3. Write result (or not) - 0
     * cycles 4. Write Z (or not) - 0 cycles 5. Write C (or not) - 0 cycles
     *
     * @param cog Cog to execute on
     */

    public boolean canExecute(Cog cog) {
        return opcode.isExecutable(cog);
    }

    public void execute(Cog cog) {
        if (this.canExecute(cog)) {
            if (this.condition.testCond(cog)) {
                boolean newz = false, newc = false;
                int avalue = destination, svalue = getSourceValue(cog);
                int result = Math.abs(svalue);
                if (this.write_result) {
                    cog.setLong(avalue, result);
                }
                if (this.write_zero) {
                    cog.setZFlag(newz);
                }
                if (this.write_carry) {
                    cog.setCFlag(newc);
                }
            }
        }
    }

    >>>>>>>
    initial readiness
    for
    instruction implementation

    public String toString() {

        String cond, opcode, dest, src;

        cond = (this.condition.equals(Condition.IF_ALWAYS)) ? "" : this.condition.name();
        opcode = this.opcode.name();
        dest = String.format("0x%H", destination);
        src = String.format("%s0x%H", (immediate) ? "#" : "", source);

        if (!this.write_result)
            switch (this.opcode) {
                case RDBYTE:
                    opcode = "WRBYTE";
                    break;
                case RDWORD:
                    opcode = "WRWORD";
                    break;
                case RDLONG:
                    opcode = "WRLONG";
                    break;
                case AND:
                    opcode = "TEST";
                    break;
                case ANDN:
                    opcode = "TESTN";
                    break;
                case SUB:
                    opcode = "CMP";
                    break;
                case SUBX:
                    opcode = "CMPX";
                    break;
                case JMPRET:
                    dest = src;
                    src = "";
                    opcode = "JMP";
                    break;
            }

        if (this.opcode.equals(OpCode.HUBOP)) {
            src = "";
            if (this.source == 0)
                opcode = "CLKSET";
            else if (this.source == 1)
                opcode = "COGID";
            else if (this.source == 2)
                opcode = "COGINIT";
            else if (this.source == 3)
                opcode = "COGSTOP";
            else if (this.source == 4)
                opcode = "LOCKNEW";
            else if (this.source == 5)
                opcode = "LOCKRET";
            else if (this.source == 6)
                opcode = "LOCKSET";
            else if (this.source == 7)
                opcode = "LOCKCLR";
        }

        return String.format(" %-13s %-7s %-5s %-5s", cond, opcode, dest, src);
    }
}
