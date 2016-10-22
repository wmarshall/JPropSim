package com.wcmarshall.jpropsim.disassembler;

import com.wcmarshall.jpropsim.Cog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Instruction {


    private static Predicate<Instruction> waitNPredicate(int n) {
        return new Predicate<Instruction>() {
            private int target = n;

            @Override
            public boolean test(Instruction i) {
                return i.getCycles() >= target;
            }

        };
    }

    private static Predicate<Instruction> isAligned = i -> i.getCog().isHubAligned();

    private static Predicate<Instruction> IOPredicate() {
        return new Predicate<Instruction>() {
            private Predicate<Instruction> wait8 = waitNPredicate(8);
            private boolean wasHubAligned = false;

            @Override
            public boolean test(Instruction i) {
                wasHubAligned = wasHubAligned || isAligned.test(i);
                return wasHubAligned && wait8.test(i);
            }
        };
    }

    private static Predicate<Instruction> waitPredicate() {
        return waitNPredicate(6);
    }

    private static BiConsumer<Cog, Instruction> incPC = (c, i) -> c.incrementPC();

    /**
     * Do not make static! Non-static prevents conditionals from affecting one another
     */
    public class OpCode {
        public OpCode ABS = new OpCode(0b101010, (
                (BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
                    int source1 = instruction.getSourceValue(cog1);
                    int dest = instruction.getDest();
                    int result = Math.abs(source1);
                    instruction.writeZ(cog1, result == 0);
                    instruction.writeC(cog1, result < 0);
                    instruction.writeResult(cog1, dest, result);
                }).andThen(incPC));

        public OpCode ABSNEG = new OpCode(0b101011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDest();
            int result = -Math.abs(source1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeC(cog1, result < 0);
            instruction.writeResult(cog1, dest, result);
        }).andThen(incPC));

        public OpCode ADD = new OpCode(0b100000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = source1 + dest;
            instruction.writeC(cog1, getUnsignedCarry(source1, dest));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode ADDABS = new OpCode(0b100010, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = Math.abs(instruction.getSourceValue(cog1));
            int dest = instruction.getDestValue(cog1);
            int result = source1 + dest;
            instruction.writeC(cog1, getUnsignedCarry(source1, dest));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode ADDS = new OpCode(0b110100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = source1 + dest;
            instruction.writeC(cog1, getSignedCarry(source1, dest));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode ADDSX = new OpCode(0b110110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            int result = source1 + dest + carry;
            instruction.writeC(cog1, getSignedCarry(source1, dest + carry) || getSignedCarry(dest, carry));
            instruction.writeZ(cog1, result == 0 && cog1.getZFlag());
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode ADDX = new OpCode(0b110010, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            int result = source1 + dest + carry;
            instruction.writeC(cog1, getUnsignedCarry(source1, dest + carry) || getUnsignedCarry(dest, carry));
            instruction.writeZ(cog1, result == 0 && cog1.getZFlag());
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode AND = new OpCode(0b011000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = source1 & dest;

            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode ANDN = new OpCode(0b011001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = ~source1 & dest;

            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode CMPS = new OpCode(0b110000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest - source1;

            instruction.writeC(cog1, dest < source1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        public OpCode CMPSUB = new OpCode(0b111000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            if (source1 <= dest) {
                // subtraction can take place
                int result = dest - source1;
                instruction.writeC(cog1, true);
                instruction.writeZ(cog1, result == 0);
                instruction.writeResult(cog1, instruction.getDest(), result);
            } else {
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, false);
            }
        }).andThen(incPC));

        public OpCode CMPSX = new OpCode(0b110001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            instruction.writeC(cog1, dest < (source1 + carry));
            instruction.writeZ(cog1, dest == (source1 + carry));
            instruction.writeResult(cog1, instruction.getDest(), dest - (source1 + carry));
        }).andThen(incPC));
        public OpCode DJNZ = new OpCode(0b111001, (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int result = instruction.getDestValue(cog1) - 1;
            instruction.writeC(cog1, result == -1);  // only situation a carry is generated
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);

            if (result != 0) {
                cog1.setPC(source1 & 0x1FF);
            } else {
                // I really don't know the proper way to do this. So I am just going to make this a NOP and re-execute
                // this should be kosher as long as the instruction object for execution is not used for display
                instruction.condition = instruction.condition.IF_NEVER;
            }
        });
        public OpCode HUBOP = new OpCode(0b000011, IOPredicate(), ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {

            int cogid, lockid;
            boolean last;

            switch (instruction.getSourceValue(cog1)) {
                case 0: // CLKSET
                    break;
                case 1: // COGID
                    cogid = cog1.getID();
                    instruction.writeResult(cog1, instruction.getDest(), cogid);
                    instruction.writeZ(cog1, cogid == 0);
                    instruction.writeC(cog1, false);
                    break;
                case 2: // COGINIT
                    cogid = cog1.getHub().initCog(instruction.getDestValue(cog1));
                    instruction.writeResult(cog1, instruction.getDest(), (cogid == -1) ? 7 : cogid);
                    instruction.writeZ(cog1, cogid == 0);
                    instruction.writeC(cog1, cogid == -1);
                    break;
                case 3: // COGSTOP
                    cogid = instruction.getDestValue(cog1);
                    instruction.writeC(cog1, cog1.getHub().stopCog(cogid));
                    instruction.writeZ(cog1, cogid == 0);
                    break;
                case 4: // LOCKNEW
                    lockid = cog1.getHub().newLock();
                    instruction.writeResult(cog1, instruction.getDest(), (lockid == -1) ? 7 : lockid);
                    instruction.writeZ(cog1, lockid == 0);
                    instruction.writeC(cog1, lockid == -1);
                    break;
                case 5: // LOCKRET
                    instruction.writeC(cog1, cog1.getHub().locksAreAvailable());
                    lockid = cog1.getHub().retLock(instruction.getDestValue(cog1) & 7);
                    instruction.writeResult(cog1, instruction.getDest(), lockid);
                    instruction.writeZ(cog1, (lockid == 0));
                    break;
                case 6: // LOCKSET
                    last = cog1.getHub().setLock(instruction.getDestValue(cog1) & 7);
                    instruction.writeResult(cog1, instruction.getDest(), instruction.getDestValue(cog1) & 7);
                    instruction.writeZ(cog1, (instruction.getDestValue(cog1) & 7) == 0);
                    instruction.writeC(cog1, last);
                    break;
                case 7: // LOCKCLR
                    last = cog1.getHub().clearLock(instruction.getDestValue(cog1) & 7);
                    instruction.writeResult(cog1, instruction.getDest(), instruction.getDestValue(cog1) & 7);
                    instruction.writeZ(cog1, (instruction.getDestValue(cog1) & 7) == 0);
                    instruction.writeC(cog1, last);
                    break;
            }
        }).andThen(incPC));
        public OpCode JMPRET = new OpCode(0b010111, (cog1, instruction) -> {
            int retInstAddr = instruction.getDest();
            int jmpAddr = instruction.getSourceValue(cog1);
            int retAddr = cog1.getPC() + 1;

            instruction.writeC(cog1, retAddr != 0);
            instruction.writeZ(cog1, false);
            // upper bits of retInstAddr remain unchanged
            retAddr = (cog1.getLong(retInstAddr) & ~0x1FF) | retAddr;
            instruction.writeResult(cog1, retInstAddr, retAddr);
            cog1.setPC(jmpAddr & 0x1FF);
        });
        public OpCode MAX = new OpCode(0b010011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeZ(cog1, source1 == 0);
            // unsigned carry returns true when a = b so ensure it is strictly a < b
            // this performs an unsigned subtraction and returns a carry when dest < source
            if (getUnsignedCarry(-dest, source1) && (source1 - dest) != 0) {
                instruction.writeC(cog1, true);
            } else {
                instruction.writeC(cog1, false);
                instruction.writeResult(cog1, instruction.getDest(), source1);
            }
        }).andThen(incPC));
        public OpCode MAXS = new OpCode(0b010001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeC(cog1, dest < source1);
            instruction.writeZ(cog1, source1 == 0);
            instruction.writeResult(cog1, instruction.getDest(), Integer.min(source1, dest));
        }).andThen(incPC));
        public OpCode MIN = new OpCode(0b010010, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeZ(cog1, source1 == 0);
            // unsigned carry returns true when a = b so ensure it is strictly a < b
            // this performs an unsigned subtraction and returns a carry when dest < source
            if (getUnsignedCarry(-dest, source1) && (source1 - dest) != 0) {
                instruction.writeC(cog1, true);
                instruction.writeResult(cog1, instruction.getDest(), source1);
            } else {
                instruction.writeC(cog1, false);
            }
        }).andThen(incPC));
        public OpCode MINS = new OpCode(0b010000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeC(cog1, dest < source1);
            instruction.writeZ(cog1, source1 == 0);
            instruction.writeResult(cog1, instruction.getDest(), Integer.max(source1, dest));
        }).andThen(incPC));
        public OpCode MOV = new OpCode(0b101000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
            instruction.writeResult(cog1, instruction.getDest(), source1);
        }).andThen(incPC));
        public OpCode MOVD = new OpCode(0b010101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int value = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            dest &= ~(0b111_111_111 << 9);
            dest |= (value & 0b111_111_111) << 9;

            instruction.writeC(cog1, dest < 0);
            instruction.writeZ(cog1, dest == 0);
            instruction.writeResult(cog1, instruction.getDest(), dest);
        }).andThen(incPC));
        public OpCode MOVI = new OpCode(0b010110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int value = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            dest &= ~(0b111_111_111 << 23);
            dest |= (value & 0b111_111_111) << 23;

            instruction.writeC(cog1, dest < 0);
            instruction.writeZ(cog1, dest == 0);
            instruction.writeResult(cog1, instruction.getDest(), dest);
        }).andThen(incPC));
        public OpCode MOVS = new OpCode(0b010100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int value = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            dest &= ~(0b111_111_111);
            dest |= (value & 0b111_111_111);

            instruction.writeC(cog1, dest < 0);
            instruction.writeZ(cog1, dest == 0);
            instruction.writeResult(cog1, instruction.getDest(), dest);
        }).andThen(incPC));
        public OpCode MUXC = new OpCode(0b011100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int mask = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest & ~mask;    // clear effected bits
            if (cog1.getCFlag())
                result = dest | mask;       // if C flag is 1, then set all effected bits to 1

            // flags are based off of final destination value, so we write destination first
            instruction.writeResult(cog1, instruction.getDest(), result);
            result = instruction.getDestValue(cog1);
            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
        }).andThen(incPC));
        public OpCode MUXNC = new OpCode(0b011101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int mask = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest & ~mask;    // clear effected bits
            if (!cog1.getCFlag())
                result = dest | mask;       // if C flag is 0, then set all effected bits to 1

            // flags are based off of final destination value, so we write destination first
            instruction.writeResult(cog1, instruction.getDest(), result);
            result = instruction.getDestValue(cog1);
            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
        }).andThen(incPC));
        public OpCode MUXNZ = new OpCode(0b011111, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int mask = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest & ~mask;    // clear effected bits
            if (!cog1.getZFlag())
                result = dest | mask;       // if Z flag is 0, then set all effected bits to 1

            // flags are based off of final destination value, so we write destination first
            instruction.writeResult(cog1, instruction.getDest(), result);
            result = instruction.getDestValue(cog1);
            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
        }).andThen(incPC));
        public OpCode MUXZ = new OpCode(0b011110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int mask = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest & ~mask;    // clear effected bits
            if (cog1.getZFlag())
                result = dest | mask;       // if Z flag is 1, then set all effected bits to 1

            // flags are based off of final destination value, so we write destination first
            instruction.writeResult(cog1, instruction.getDest(), result);
            result = instruction.getDestValue(cog1);
            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
        }).andThen(incPC));
        public OpCode NEG = new OpCode(0b101001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);

            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
            instruction.writeResult(cog1, instruction.getDest(), -source1);
        }).andThen(incPC));
        public OpCode NEGC = new OpCode(0b101100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int result = ((cog1.getCFlag()) ? -1 : 1) * source1;

            instruction.writeResult(cog1, instruction.getDest(), result);
            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
        }).andThen(incPC));
        public OpCode NEGNC = new OpCode(0b101101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int result = ((cog1.getCFlag()) ? 1 : -1) * source1;

            instruction.writeResult(cog1, instruction.getDest(), result);
            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
        }).andThen(incPC));
        public OpCode NEGNZ = new OpCode(0b101111, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int result = ((cog1.getZFlag()) ? 1 : -1) * source1;

            instruction.writeResult(cog1, instruction.getDest(), result);
            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
        }).andThen(incPC));
        public OpCode NEGZ = new OpCode(0b101110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int result = ((cog1.getZFlag()) ? -1 : 1) * source1;

            instruction.writeResult(cog1, instruction.getDest(), result);
            instruction.writeC(cog1, source1 < 0);
            instruction.writeZ(cog1, source1 == 0);
        }).andThen(incPC));
        public OpCode OR = new OpCode(0b011010, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = source1 | dest;

            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode RCL = new OpCode(0b001101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            boolean newCarry = value < 0;
            int mask = 0;
            for (int i = 0; i < bitCount; i++)
                mask = (mask << 1) | carry;
            value = (value << bitCount) | mask;

            instruction.writeC(cog1, newCarry);
            instruction.writeZ(cog1, value == 0);
            instruction.writeResult(cog1, instruction.getDest(), value);
        }).andThen(incPC));
        public OpCode RCR = new OpCode(0b001100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            boolean newCarry = (value & 1) == 1;
            // set only the top most bit to 1 if carry is set
            int mask = (cog1.getCFlag()) ? Integer.MIN_VALUE : 0;
            // shift the mask down, sign extending to replace the upper bits
            mask >>= bitCount - 1;
            value = (value >>> bitCount) | mask;

            instruction.writeC(cog1, newCarry);
            instruction.writeZ(cog1, value == 0);
            instruction.writeResult(cog1, instruction.getDest(), value);
        }).andThen(incPC));
        public OpCode RDBYTE = new OpCode(0b000000, IOPredicate(), ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int hubAddr = instruction.getSourceValue(cog1);
            int cogAddr = instruction.getDest();
            if (instruction.write_result) {     // RDBYTE
                int value = cog1.getHub().getByte(hubAddr);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, value == 0);
                instruction.writeResult(cog1, cogAddr, value);
            } else {                            // WRBYTE
                int value = cog1.getLong(cogAddr);
                cog1.getHub().setByte(hubAddr, value);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, (hubAddr & 0b11) != 0);
            }
        }).andThen(incPC));
        public OpCode RDLONG = new OpCode(0b000010, IOPredicate(), ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int hubAddr = instruction.getSourceValue(cog1);
            int cogAddr = instruction.getDest();
            if (instruction.write_result) {     // RDLONG
                int value = cog1.getHub().getLong(hubAddr);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, value == 0);
                instruction.writeResult(cog1, cogAddr, value);
            } else {                            // WRLONG
                int value = cog1.getLong(cogAddr);
                cog1.getHub().setLong(hubAddr, value);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, (hubAddr & 0b11) != 0);
            }
        }).andThen(incPC));
        public OpCode RDWORD = new OpCode(0b000001, IOPredicate(), ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int hubAddr = instruction.getSourceValue(cog1);
            int cogAddr = instruction.getDest();
            if (instruction.write_result) {     // RDWORD
                int value = cog1.getHub().getWord(hubAddr);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, value == 0);
                instruction.writeResult(cog1, cogAddr, value);
            } else {                            // WRWORD
                int value = cog1.getLong(cogAddr);
                cog1.getHub().setWord(hubAddr, value);
                instruction.writeC(cog1, false);
                instruction.writeZ(cog1, (hubAddr & 0b1) != 0);
            }
        }).andThen(incPC));
        public OpCode REV = new OpCode(0b001111, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = 32 - (instruction.getSourceValue(cog1) & 0b11111);
            int value = instruction.getDestValue(cog1);
            int result = 0;
            for (int i = 0; i < bitCount; i++)
                result |= ((value & (1 << i)) >> i) << (bitCount - i - 1);
            instruction.writeC(cog1, (value & 1) == 1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode ROL = new OpCode(0b001001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int result = (value << bitCount) | (value >>> (32 - bitCount));

            instruction.writeC(cog1, value < 0);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode ROR = new OpCode(0b001000, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int result = (value >>> bitCount) | (value << (32 - bitCount));

            instruction.writeC(cog1, (value & 1) == 1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SAR = new OpCode(0b001110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int result = value >> bitCount;

            instruction.writeC(cog1, (value & 1) == 1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SHL = new OpCode(0b001011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int result = value << bitCount;

            instruction.writeC(cog1, value < 0);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SHR = new OpCode(0b001010, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int bitCount = instruction.getSourceValue(cog1) & 0b11111;
            int value = instruction.getDestValue(cog1);
            int result = value >>> bitCount;

            instruction.writeC(cog1, (value & 1) == 1);
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUB = new OpCode(0b100001, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest - source1;

            instruction.writeC(cog1, getUnsignedCarry(-dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUBABS = new OpCode(0b100011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = Math.abs(instruction.getSourceValue(cog1));
            int dest = instruction.getDestValue(cog1);
            int result = dest - source1;

            instruction.writeC(cog1, getUnsignedCarry(-dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUBS = new OpCode(0b110101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = Math.abs(instruction.getSourceValue(cog1));
            int dest = instruction.getDestValue(cog1);
            int result = dest - source1;

            instruction.writeC(cog1, getSignedCarry(dest, -source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUBSX = new OpCode(0b110111, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = Math.abs(instruction.getSourceValue(cog1));
            int dest = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            int result = dest - (source1 + carry);

            instruction.writeC(cog1, getSignedCarry(dest, -(source1 + carry)));
            instruction.writeZ(cog1, result == 0 && cog1.getZFlag());
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUBX = new OpCode(0b110011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = Math.abs(instruction.getSourceValue(cog1));
            int dest = instruction.getDestValue(cog1);
            int carry = (cog1.getCFlag()) ? 1 : 0;
            int result = dest - (source1 + carry);

            instruction.writeC(cog1, getUnsignedCarry(-dest, source1 + carry));
            instruction.writeZ(cog1, result == 0 && cog1.getZFlag());
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUMC = new OpCode(0b100100, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = ((cog1.getCFlag()) ? -1 : 1) * instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest + source1;

            instruction.writeC(cog1, getSignedCarry(dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUMNC = new OpCode(0b100101, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = ((cog1.getCFlag()) ? 1 : -1) * instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest + source1;

            instruction.writeC(cog1, getSignedCarry(dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUMNZ = new OpCode(0b100111, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = ((cog1.getZFlag()) ? 1 : -1) * instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest + source1;

            instruction.writeC(cog1, getSignedCarry(dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode SUMZ = new OpCode(0b100110, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = ((cog1.getZFlag()) ? -1 : 1) * instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = dest + source1;

            instruction.writeC(cog1, getSignedCarry(dest, source1));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));
        public OpCode TJNZ = new OpCode(0b111010, (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeC(cog1, false);
            instruction.writeZ(cog1, dest == 0);
            // R is not defined for TJNZ
            //instruction.writeResult(cog, instruction.getDest(), dest);

            if (dest != 0) {
                cog1.setPC(source1 & 0x1FF);
            } else {
                // same mechanism as DJNZ
                instruction.condition = instruction.condition.IF_NEVER;
            }
        });
        public OpCode TJZ = new OpCode(0b111011, (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            instruction.writeC(cog1, false);
            instruction.writeZ(cog1, dest == 0);
            // R is not defined for TJZ
            //instruction.writeResult(cog, instruction.getDest(), dest);

            if (dest == 0) {
                cog1.setPC(source1 & 0x1FF);
            } else {
                // same mechanism as DJNZ
                instruction.condition = instruction.condition.IF_NEVER;
            }
        });
        public OpCode WAITCNT = new OpCode(0b111110, waitPredicate(), (cog1, instruction) -> {
            int cnt = cog1.getCnt();
            int delta = instruction.getSourceValue(cog1);
            int target = instruction.getDestValue(cog1);
            int result = target + delta;

            if (cnt != target) return;

            instruction.writeC(cog1, getUnsignedCarry(target, delta));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
            incPC.accept(cog1, instruction);
        });
        public OpCode WAITPEQ = new OpCode(0b111100, waitPredicate(), (cog1, instruction) -> {
            int current = cog1.getINA();
            int target = instruction.getDestValue(cog1);
            int mask = instruction.getSourceValue(cog1);

            if ((current & mask) != target) return;

            instruction.writeC(cog1, false);
            // ?!?!?!?!?! this is the actual behavior, shotty documentation
            instruction.writeZ(cog1, target + mask == 0);
            instruction.writeResult(cog1, instruction.getDest(), target + mask);
        });
        public OpCode WAITPNE = new OpCode(0b111101, waitPredicate(), (cog1, instruction) -> {
            int current = cog1.getINA();
            int target = instruction.getDestValue(cog1);
            int mask = instruction.getSourceValue(cog1);

            if ((current & mask) == target) return;

            // "whatever it naturally ended up being"
            instruction.writeC(cog1, target + mask + 1 == 0);
            instruction.writeZ(cog1, target + mask + 1 == 0);
            instruction.writeResult(cog1, instruction.getDest(), target + mask + 1);
        });
        public OpCode WAITVID = new OpCode(0b111111, (cog1, instruction) -> {
            // TODO
        });
        public OpCode XOR = new OpCode(0b011011, ((BiConsumer<Cog, Instruction>) (cog1, instruction) -> {
            int source1 = instruction.getSourceValue(cog1);
            int dest = instruction.getDestValue(cog1);
            int result = source1 ^ dest;

            instruction.writeC(cog1, getParity(result));
            instruction.writeZ(cog1, result == 0);
            instruction.writeResult(cog1, instruction.getDest(), result);
        }).andThen(incPC));

        private final int instr;

        private final BiConsumer<Cog, Instruction> exec_fn;

        private final Predicate<Instruction> executable;

        private OpCode(int instr, Predicate<Instruction> executable, BiConsumer<Cog, Instruction> exec_fn) {
            this.instr = instr;
            this.exec_fn = exec_fn;
            this.executable = executable;
        }

        private OpCode(int instr, BiConsumer<Cog, Instruction> exec_fn) {
            this(instr, waitNPredicate(4), exec_fn);
        }

        private OpCode(int instr, Predicate<Instruction> executable) {
            this(instr, executable, null);
        }

        private OpCode(int instr) {
            this(instr, waitNPredicate(4));
        }

        public OpCode() {
            //Return a sad, broken opcode
            this.instr = 0;
            this.exec_fn = null;
            this.executable = null;
        }

        public ArrayList<OpCode> values() {
            ArrayList<OpCode> retval = new ArrayList<>();
            for (Field f : this.getClass().getFields()) {
                if (f.getType().equals(this.getClass())) {
                    try {
                        retval.add((OpCode) f.get(this));
                    } catch (IllegalAccessException e) {

                    }
                }
            }
            return retval;
        }

        public String name() {
            for (Field f : this.getClass().getFields()) {
                if (f.getType().equals(this.getClass())) {
                    try {
                        if (f.get(this).equals(this)) {
                            return f.getName();
                        }
                    } catch (IllegalAccessException e) {
                        //pass
                    }
                }
            }
            return "";
        }

        public int getInstr() {
            return this.instr;
        }

        public boolean isExecutable(Instruction i) {
            return executable.test(i);
        }

        public void execute(Cog cog, Instruction ins) {
            exec_fn.accept(cog, ins);
        }

        /**
         * Calculates the carry bit resulting from the addition of two signed integers a & b
         * This assumes that the formula is a+b. Therefore to calculate carry from a-b, pass a and -b
         *
         * @param a first element of addition
         * @param b second element of addition
         * @return returns whether or not a carry bit is generated from a+b
         */
        public boolean getSignedCarry(int a, int b) {
            if (a > 0 && b > Integer.MAX_VALUE - a)
                return true;
            if (a < 0 && b < Integer.MIN_VALUE - a)
                return true;
            return false;
        }

        /**
         * Calculates the carry bit resulting from the addition of two unsigned integers a & b
         * This assumes that the formula is a+b. To calculate the carry from a-b, pass -a and b
         *
         * @param a first element of addition
         * @param b second element of addition
         * @return returns whether or not a carry bit is generated from a+b
         */
        public boolean getUnsignedCarry(int a, int b) {
            if (a < 0 && b < 0)
                return true;
            if (b < 0 && b >= -a)
                return true;
            if (a < 0 && b >= -a)
                return true;
            return false;
        }

        public boolean getParity(int n) {
            int parity = 0;
            for (int i = 0; i < 32; i++)
                parity ^= (n >> i) & 1;
            return parity == 1;
        }

    }

    private static final Predicate<Cog> ifZ = c -> c.getZFlag();
    private static final Predicate<Cog> ifC = c -> c.getCFlag();

    /**
     * Do not make static! Non-static prevents conditionals from affecting one another
     */
    public class Condition {

        public Condition IF_ALWAYS = new Condition(0b1111, c -> true);
        public Condition IF_NEVER = new Condition(0b0000, c -> false);
        public Condition IF_Z = new Condition(0b1010, ifZ);
        public Condition IF_NZ = new Condition(0b0101, ifZ.negate());
        public Condition IF_C = new Condition(0b1100, ifC);
        public Condition IF_NC = new Condition(0b0011, ifC.negate());
        public Condition IF_Z_AND_C = new Condition(0b1000, ifZ.and(ifC));
        public Condition IF_Z_OR_C = new Condition(0b1110, ifZ.or(ifC));
        public Condition IF_Z_EQ_C = new Condition(0b1001, c -> c.getZFlag() == c.getCFlag());
        public Condition IF_Z_NE_C = new Condition(0b0110, c -> c.getZFlag() != c.getCFlag());
        public Condition IF_NZ_AND_NC = new Condition(0b0001, ifZ.negate().and(ifC.negate()));
        public Condition IF_NZ_OR_NC = new Condition(0b0111, ifZ.negate().or(ifC.negate()));
        public Condition IF_Z_AND_NC = new Condition(0b0010, ifZ.and(ifC.negate()));
        public Condition IF_Z_OR_NC = new Condition(0b1011, ifZ.or(ifC.negate()));
        public Condition IF_NZ_AND_C = new Condition(0b0100, ifZ.negate().and(ifC));
        public Condition IF_NZ_OR_C = new Condition(0b1101, ifZ.negate().or(ifC));

        private final int cond;

        private final Predicate<Cog> test;

        private Condition(int cond, Predicate<Cog> test) {
            this.cond = cond;
            this.test = test;
        }

        public Condition() {
            this.cond = 0;
            this.test = null;
        }

        public int getCond() {
            return this.cond;
        }

        public boolean testCond(Cog c) {
            return test != null && test.test(c);
        }

        public ArrayList<Condition> values() {
            ArrayList<Condition> retval = new ArrayList<>();
            for (Field f : this.getClass().getFields()) {
                if (f.getType().equals(this.getClass())) {
                    try {
                        retval.add((Condition) f.get(this));
                    } catch (IllegalAccessException e) {

                    }
                }
            }
            return retval;
        }

        public String name() {
            for (Field f : this.getClass().getFields()) {
                if (f.getType().equals(this.getClass())) {
                    try {
                        if (f.get(this).equals(this)) {
                            return f.getName();
                        }
                    } catch (IllegalAccessException e) {
                        //pass
                    }
                }
            }
            return "";
        }
    }

    private Cog cog;

    private OpCode opcode;

    private boolean write_zero, write_carry, write_result, immediate;

    private Condition condition;

    private int destination, source, encodedInstr, cycleCount;

    private final Predicate<Instruction> NOPPredicate = waitNPredicate(4);

    public Instruction(Cog c, int encoded) {
        int instr = (encoded >> (32 - 6)) & 0b111111;
        for (OpCode opcode : new OpCode().values()) {
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
        for (Condition condition : new Condition().values()) {
            if (cond == condition.getCond()) {
                this.condition = condition;
            }
        }
        this.cog = c;
        this.cycleCount = 0;
        this.encodedInstr = encoded;
        this.destination = (encoded >> (32 - 6 - 4 - 4 - 9)) & 0b111111111;
        this.source = (encoded >> (32 - 6 - 4 - 4 - 9 - 9)) & 0b111111111;
    }

    public int getEncodedInstr() {
        return encodedInstr;
    }

    public int getDest() {
        return destination;
    }

    public int getDestValue(Cog cog) {
        return cog.getLong(this.destination);
    }

    public int getSourceValue(Cog cog) {
        if (this.immediate) {
            return source;
        }
        return cog.getLong(this.source);
    }

    public Cog getCog() {
        return cog;
    }

    public int getCycles() {
        return cycleCount;
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

    private void writeResult(Cog cog, int addr, int result) {
        if (this.write_result) {
            cog.setLong(addr, result);
        }
    }

    /**
     * Execution occurs in phases 1. Test Condition - 0 cycles 2. perform
     * operation (or do nothing) - 4-23 cycles 3. Write result (or not) - 0
     * cycles 4. Write Z (or not) - 0 cycles 5. Write C (or not) - 0 cycles
     */

    public boolean canExecute() {
        return opcode.isExecutable(this);
    }

    /**
     * Called once per clock tick
     * Updates cog.pc when it's time to move on
     */
    public void execute() {
        cycleCount++;
        if (!this.condition.testCond(cog)) {
            if (NOPPredicate.test(this)) {
                incPC.accept(cog, this);
            }
        } else {
            if (this.canExecute()) {
                this.opcode.execute(cog, this);
            }
        }
    }

    private static String register2string(int register, boolean immediate) {
        switch (register) {
            case 0x1F0:
                return "PAR";
            case 0x1F1:
                return "CNT";
            case 0x1F2:
                return "INA";
            case 0x1F3:
                return "INB";
            case 0x1F4:
                return "OUTA";
            case 0x1F5:
                return "OUTB";
            case 0x1F6:
                return "DIRA";
            case 0x1F7:
                return "DIRB";
            case 0x1F8:
                return "CTRA";
            case 0x1F9:
                return "CTRB";
            case 0x1FA:
                return "FRQA";
            case 0x1FB:
                return "FRQB";
            case 0x1FC:
                return "PHSA";
            case 0x1FD:
                return "PHSB";
            case 0x1FE:
                return "VCFG";
            case 0x1FF:
                return "VSCL";
            default:
                return String.format("%s0x%03X", (immediate) ? "#" : "", register);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof Instruction)
            return ((Instruction) o).encodedInstr == this.encodedInstr;

        if (o instanceof Integer)
            return o.equals(this.encodedInstr);

        return false;
    }

    public String toString() {

        String cond, opcode, dest, src, effects;

        if (this.opcode == null) {
            return String.format("               %02X  %02X  %02X  %02X", encodedInstr & 0xFF,
                    (encodedInstr >> 8) & 0xFF, (encodedInstr >> 16) & 0xFF, (encodedInstr >> 24) & 0xFF);
        }


        opcode = this.opcode.name();
        cond = (this.condition.equals(this.condition.IF_ALWAYS)) ? "" : this.condition.name();
        dest = register2string(destination, false);
        src = register2string(source, immediate);
        effects = (this.write_result) ? "wr" : "nr";
        effects += ((this.write_carry) ? ",wc" : "") + ((this.write_zero) ? ",wz" : "");

        if (!this.write_result) {
            if (this.opcode == this.opcode.RDBYTE) {
                opcode = "WRBYTE";
            } else if (this.opcode == this.opcode.RDWORD) {
                opcode = "WRWORD";
            } else if (this.opcode == this.opcode.RDLONG) {
                opcode = "WRLONG";
            } else if (this.opcode == this.opcode.AND) {
                opcode = "TEST";
            } else if (this.opcode == this.opcode.ANDN) {
                opcode = "TESTN";
            } else if (this.opcode == this.opcode.SUB) {
                opcode = "CMP";
            } else if (this.opcode == this.opcode.SUBX) {
                opcode = "CMPX";
            } else if (this.opcode == this.opcode.JMPRET) {
                dest = src;
                src = "";
                opcode = "JMP";
            }
        }

        if (this.opcode == this.opcode.HUBOP)

        {
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

        return String.format(" %-13s %-7s %-6s %-6s %s", cond, opcode, dest, src, effects);
    }
}
