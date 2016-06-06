package com.wcmarshall.jpropsim.disassembler;

import com.wcmarshall.jpropsim.Cog;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Instruction {


    private static Predicate<Cog> waitNPredicate(int n) {
        return new Predicate<Cog>() {
            private int count = n - 1;

            @Override
            public boolean test(Cog c) {
                return ((count > 0) ? (count--) : count) == 0;
            }

        };
    }

    private static Predicate<Cog> isAligned = c -> c.isHubAligned();

    private static Predicate<Cog> IOPredicate() {
        return new Predicate<Cog>() {
            private Predicate<Cog> wait8 = waitNPredicate(8);
            private boolean wasHubAligned = false;

            @Override
            public boolean test(Cog cog) {
                wasHubAligned = wasHubAligned || isAligned.test(cog);
                if (wasHubAligned) {
                    return wait8.test(cog);
                } else {
                    return false;
                }
            }
        };
    }

    private static Predicate<Cog> waitPredicate() {
        return waitNPredicate(6);
    }

    private static BiConsumer<Cog, Instruction> incPC = (c, i) -> c.incrementPC();

    /**
     * Do not make static! Non-static prevents conditionals from affecting one another
     */
    public enum OpCode {
        ABS(0b101010, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDest();
                int result = Math.abs(source);
                instruction.writeZ(cog, result == 0);
                instruction.writeC(cog, result < 0);
                instruction.writeResult(cog, dest, result);
            }
        }.andThen(incPC)),
        ABSNEG(0b101011, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDest();
				int result = -Math.abs(source);
				instruction.writeZ(cog, result == 0);
				instruction.writeC(cog, result < 0);
				instruction.writeResult(cog, dest, result);
			}
		}.andThen(incPC)),
        ADD(0b100000, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int result = source + dest;
				instruction.writeC(cog, getUnsignedCarry(source, dest));
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        ADDABS(0b100010, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = Math.abs(instruction.getSourceValue(cog));
				int dest = instruction.getDestValue(cog);
				int result = source + dest;
				instruction.writeC(cog, getUnsignedCarry(source, dest));
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        ADDS(0b110100, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int result = source + dest;
				instruction.writeC(cog, getSignedCarry(source, dest));
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        ADDSX(0b110110, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int carry = (cog.getCFlag()) ? 1 : 0;
				int result = source + dest + carry;
				instruction.writeC(cog, getSignedCarry(source, dest+carry));
				instruction.writeZ(cog, result == 0 && cog.getZFlag());
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        ADDX(0b110010, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int carry = (cog.getCFlag()) ? 1 : 0;
				int result = source + dest + carry;
				instruction.writeC(cog, getUnsignedCarry(source, dest+carry));
				instruction.writeZ(cog, result == 0 && cog.getZFlag());
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        AND(0b011000, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int result = source & dest;

				instruction.writeC(cog, getParity(result));
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        ANDN(0b011001, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int result = ~source & dest;

				instruction.writeC(cog, getParity(result));
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        CMPS(0b110000, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
				int source = instruction.getSourceValue(cog);
				int dest = instruction.getDestValue(cog);
				int result = dest - source;

				instruction.writeC(cog, dest < source);
				instruction.writeZ(cog, result == 0);
				instruction.writeResult(cog, instruction.getDest(), result);
			}
		}.andThen(incPC)),
        CMPSUB(0b111000, new BiConsumer<Cog, Instruction>() {
			@Override
			public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                if (source <= dest) {
                    // subtraction can take place
                    int result = dest - source;
                    instruction.writeC(cog, true);
                    instruction.writeZ(cog, result == 0);
                    instruction.writeResult(cog, instruction.getDest(), result);
                } else {
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, false);
                }
			}
		}.andThen(incPC)),
        CMPSX(0b110001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int carry = (cog.getCFlag()) ? 1 : 0;
                instruction.writeC(cog, dest < (source + carry));
                instruction.writeZ(cog, dest == (source + carry));
                instruction.writeResult(cog, instruction.getDest(), dest - (source + carry));
            }
        }.andThen(incPC)),
        DJNZ(0b111001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int result = instruction.getDestValue(cog) - 1;
                instruction.writeC(cog, result == -1);  // only situation a carry is generated
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);

                if (result == 0) {
                    cog.setPC(source & 0x1FF);
                } else {
                    // I really don't know the proper way to do this. So I am just going to make this a NOP and re-execute
                    // this should be kosher as long as the instruction object for execution is not used for display
                    instruction.condition = Condition.IF_NEVER;
                }
            }
        }),
        HUBOP(0b000011, IOPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                // TODO create mechanism for HUBOP
                switch (instruction.getSourceValue(cog)) {
                    case 0: // CLKSET
                        break;
                    case 1: // COGID
                        break;
                    case 2: // COGINIT
                        break;
                    case 3: // COGSTOP
                        break;
                    case 4: // LOCKNEW
                        break;
                    case 5: // LOCKRET
                        break;
                    case 6: // LOCKSET
                        break;
                    case 7: // LOCKCLR
                        break;
                }
            }
        }.andThen(incPC)),
        JMPRET(0b010111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int retInstAddr = instruction.getDest();
                int jmpAddr = instruction.getSourceValue(cog);
                int retAddr = cog.getPC() + 1;

                instruction.writeC(cog, retAddr != 0);
                instruction.writeZ(cog, false);
                // upper bits of retInstAddr remain unchanged
                retAddr = (cog.getLong(retInstAddr) & ~0x1FF) | retAddr;
                instruction.writeResult(cog, retInstAddr, retAddr);
                cog.setPC(jmpAddr & 0x1FF);
            }
        }),
        MAX(0b010011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeZ(cog, source == 0);
                // unsigned carry returns true when a = b so ensure it is strictly a < b
                // this performs an unsigned subtraction and returns a carry when dest < source
                if (getUnsignedCarry(-dest, source) && (source - dest) != 0) {
                    instruction.writeC(cog, true);
                } else {
                    instruction.writeC(cog, false);
                    instruction.writeResult(cog, instruction.getDest(), source);
                }
            }
        }.andThen(incPC)),
        MAXS(0b010001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeC(cog, dest < source);
                instruction.writeZ(cog, source == 0);
                instruction.writeResult(cog, instruction.getDest(), Integer.min(source, dest));
            }
        }.andThen(incPC)),
        MIN(0b010010, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeZ(cog, source == 0);
                // unsigned carry returns true when a = b so ensure it is strictly a < b
                // this performs an unsigned subtraction and returns a carry when dest < source
                if (getUnsignedCarry(-dest, source) && (source - dest) != 0) {
                    instruction.writeC(cog, true);
                    instruction.writeResult(cog, instruction.getDest(), source);
                } else {
                    instruction.writeC(cog, false);
                }
            }
        }.andThen(incPC)),
        MINS(0b010000, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeC(cog, dest < source);
                instruction.writeZ(cog, source == 0);
                instruction.writeResult(cog, instruction.getDest(), Integer.max(source, dest));
            }
        }.andThen(incPC)),
        MOV(0b101000, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
                instruction.writeResult(cog, instruction.getDest(), source);
            }
        }.andThen(incPC)),
        MOVD(0b010101, new BiConsumer<Cog, Instruction>() {
            // TODO make mechanism for pipeline
            @Override
            public void accept(Cog cog, Instruction instruction) {

            }
        }.andThen(incPC)),
        MOVI(0b010110, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {

            }
        }.andThen(incPC)),
        MOVS(0b010100, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {

            }
        }.andThen(incPC)),
        MUXC(0b011100, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int mask = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest & ~mask;    // clear effected bits
                if (cog.getCFlag())
                    result = dest | mask;       // if C flag is 1, then set all effected bits to 1

                // flags are based off of final destination value, so we write destination first
                instruction.writeResult(cog, instruction.getDest(), result);
                result = instruction.getDestValue(cog);
                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
            }
        }.andThen(incPC)),
        MUXNC(0b011101, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int mask = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest & ~mask;    // clear effected bits
                if (!cog.getCFlag())
                    result = dest | mask;       // if C flag is 0, then set all effected bits to 1

                // flags are based off of final destination value, so we write destination first
                instruction.writeResult(cog, instruction.getDest(), result);
                result = instruction.getDestValue(cog);
                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
            }
        }.andThen(incPC)),
        MUXNZ(0b011111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int mask = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest & ~mask;    // clear effected bits
                if (!cog.getZFlag())
                    result = dest | mask;       // if Z flag is 0, then set all effected bits to 1

                // flags are based off of final destination value, so we write destination first
                instruction.writeResult(cog, instruction.getDest(), result);
                result = instruction.getDestValue(cog);
                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
            }
        }.andThen(incPC)),
        MUXZ(0b011110, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int mask = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest & ~mask;    // clear effected bits
                if (cog.getZFlag())
                    result = dest | mask;       // if Z flag is 1, then set all effected bits to 1

                // flags are based off of final destination value, so we write destination first
                instruction.writeResult(cog, instruction.getDest(), result);
                result = instruction.getDestValue(cog);
                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
            }
        }.andThen(incPC)),
        NEG(0b101001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);

                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
                instruction.writeResult(cog, instruction.getDest(), -source);
            }
        }.andThen(incPC)),
        NEGC(0b101100, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int result = ((cog.getCFlag()) ? -1 : 1) * source;

                instruction.writeResult(cog, instruction.getDest(), result);
                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
            }
        }.andThen(incPC)),
        NEGNC(0b101101, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int result = ((cog.getCFlag()) ? 1 : -1) * source;

                instruction.writeResult(cog, instruction.getDest(), result);
                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
            }
        }.andThen(incPC)),
        NEGNZ(0b101111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int result = ((cog.getZFlag()) ? 1 : -1) * source;

                instruction.writeResult(cog, instruction.getDest(), result);
                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
            }
        }.andThen(incPC)),
        NEGZ(0b101110, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int result = ((cog.getZFlag()) ? -1 : 1) * source;

                instruction.writeResult(cog, instruction.getDest(), result);
                instruction.writeC(cog, source < 0);
                instruction.writeZ(cog, source == 0);
            }
        }.andThen(incPC)),
        OR(0b011010, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = source | dest;

                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        RCL(0b001101, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int carry = (cog.getCFlag()) ? 1 : 0;
                boolean newCarry = value < 0;
                int mask = 0;
                for (int i=0; i<bitCount; i++)
                    mask = (mask << 1) | carry;
                value = (value << bitCount) | mask;

                instruction.writeC(cog, newCarry);
                instruction.writeZ(cog, value == 0);
                instruction.writeResult(cog, instruction.getDest(), value);
            }
        }.andThen(incPC)),
        RCR(0b001100, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                boolean newCarry = (value & 1) == 1;
                // set only the top most bit to 1 if carry is set
                int mask = (cog.getCFlag()) ? Integer.MIN_VALUE : 0;
                // shift the mask down, sign extending to replace the upper bits
                mask >>= bitCount - 1;
                value = (value >>> bitCount) | mask;

                instruction.writeC(cog, newCarry);
                instruction.writeZ(cog, value == 0);
                instruction.writeResult(cog, instruction.getDest(), value);
            }
        }.andThen(incPC)),
        RDBYTE(0b000000, IOPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int hubAddr = instruction.getSourceValue(cog);
                int cogAddr = instruction.getDest();
                if (instruction.write_result) {     // RDBYTE
                    int value = cog.getHub().getByte(hubAddr);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, value == 0);
                    instruction.writeResult(cog, cogAddr, value);
                } else {                            // WRBYTE
                    int value = cog.getLong(cogAddr);
                    cog.getHub().setByte(hubAddr, value);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, (hubAddr & 0b11) != 0);
                }
            }
        }.andThen(incPC)),
        RDLONG(0b000010, IOPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int hubAddr = instruction.getSourceValue(cog);
                int cogAddr = instruction.getDest();
                if (instruction.write_result) {     // RDLONG
                    int value = cog.getHub().getLong(hubAddr);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, value == 0);
                    instruction.writeResult(cog, cogAddr, value);
                } else {                            // WRLONG
                    int value = cog.getLong(cogAddr);
                    cog.getHub().setLong(hubAddr, value);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, (hubAddr & 0b11) != 0);
                }
            }
        }.andThen(incPC)),
        RDWORD(0b000001, IOPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int hubAddr = instruction.getSourceValue(cog);
                int cogAddr = instruction.getDest();
                if (instruction.write_result) {     // RDWORD
                    int value = cog.getHub().getWord(hubAddr);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, value == 0);
                    instruction.writeResult(cog, cogAddr, value);
                } else {                            // WRWORD
                    int value = cog.getLong(cogAddr);
                    cog.getHub().setWord(hubAddr, value);
                    instruction.writeC(cog, false);
                    instruction.writeZ(cog, (hubAddr & 0b1) != 0);
                }
            }
        }.andThen(incPC)),
        REV(0b001111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = 32 - (instruction.getSourceValue(cog) & 0b11111);
                int value = instruction.getDestValue(cog);
                int result = 0;
                for (int i=0; i<bitCount; i++)
                    result |= ((value & (1 << i)) >> i) << (bitCount - i - 1);
                instruction.writeC(cog, (value & 1) == 1);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        ROL(0b001001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int result = (value << bitCount) | (value >>> (32 - bitCount));

                instruction.writeC(cog, value < 0);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        ROR(0b001000, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int result = (value >>> bitCount) | (value << (32 - bitCount));

                instruction.writeC(cog, (value & 1) == 1);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SAR(0b001110, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int result = value >> bitCount;

                instruction.writeC(cog, (value & 1) == 1);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SHL(0b001011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int result = value << bitCount;

                instruction.writeC(cog, value < 0);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SHR(0b001010, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int bitCount = instruction.getSourceValue(cog) & 0b11111;
                int value = instruction.getDestValue(cog);
                int result = value >>> bitCount;

                instruction.writeC(cog, (value & 1) == 1);
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUB(0b100001, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest - source;

                instruction.writeC(cog, getUnsignedCarry(-dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUBABS(0b100011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = Math.abs(instruction.getSourceValue(cog));
                int dest = instruction.getDestValue(cog);
                int result = dest - source;

                instruction.writeC(cog, getUnsignedCarry(-dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUBS(0b110101, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = Math.abs(instruction.getSourceValue(cog));
                int dest = instruction.getDestValue(cog);
                int result = dest - source;

                instruction.writeC(cog, getSignedCarry(dest, -source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUBSX(0b110111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = Math.abs(instruction.getSourceValue(cog));
                int dest = instruction.getDestValue(cog);
                int carry = (cog.getCFlag()) ? 1 : 0;
                int result = dest - (source + carry);

                instruction.writeC(cog, getSignedCarry(dest, -(source + carry)));
                instruction.writeZ(cog, result == 0 && cog.getZFlag());
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUBX(0b110011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = Math.abs(instruction.getSourceValue(cog));
                int dest = instruction.getDestValue(cog);
                int carry = (cog.getCFlag()) ? 1 : 0;
                int result = dest - (source + carry);

                instruction.writeC(cog, getUnsignedCarry(-dest, source + carry));
                instruction.writeZ(cog, result == 0 && cog.getZFlag());
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUMC(0b100100, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = ((cog.getCFlag()) ? -1 : 1) * instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest + source;

                instruction.writeC(cog, getSignedCarry(dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUMNC(0b100101, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = ((cog.getCFlag()) ? 1 : -1) * instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest + source;

                instruction.writeC(cog, getSignedCarry(dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUMNZ(0b100111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = ((cog.getZFlag()) ? 1 : -1) * instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest + source;

                instruction.writeC(cog, getSignedCarry(dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        SUMZ(0b100110, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = ((cog.getZFlag()) ? -1 : 1) * instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = dest + source;

                instruction.writeC(cog, getSignedCarry(dest, source));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC)),
        TJNZ(0b111010, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeC(cog, false);
                instruction.writeZ(cog, dest == 0);
                // R is not defined for TJNZ
                //instruction.writeResult(cog, instruction.getDest(), dest);

                if (dest != 0) {
                    cog.setPC(source & 0x1FF);
                } else {
                    // same mechanism as DJNZ
                    instruction.condition = Condition.IF_NEVER;
                }
            }
        }),
        TJZ(0b111011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                instruction.writeC(cog, false);
                instruction.writeZ(cog, dest == 0);
                // R is not defined for TJZ
                //instruction.writeResult(cog, instruction.getDest(), dest);

                if (dest == 0) {
                    cog.setPC(source & 0x1FF);
                } else {
                    // same mechanism as DJNZ
                    instruction.condition = Condition.IF_NEVER;
                }
            }
        }),
        WAITCNT(0b111110, waitPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int cnt = cog.getCnt();
                int delta = instruction.getSourceValue(cog);
                int target = instruction.getDestValue(cog);
                int result = target + delta;

                if (cnt != target) return;

                instruction.writeC(cog, getUnsignedCarry(target, delta));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
                incPC.accept(cog, instruction);
            }
        }),
        WAITPEQ(0b111100, waitPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int current = cog.getINA();
                int target = instruction.getDestValue(cog);
                int mask = instruction.getSourceValue(cog);

                if ((current & mask) != target) return;

                instruction.writeC(cog, false);
                // ?!?!?!?!?! this is the actual behavior, shotty documentation
                instruction.writeZ(cog, target + mask == 0);
                instruction.writeResult(cog, instruction.getDest(), target+mask);
            }
        }),
        WAITPNE(0b111101, waitPredicate(), new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int current = cog.getINA();
                int target = instruction.getDestValue(cog);
                int mask = instruction.getSourceValue(cog);

                if ((current & mask) == target) return;

                // "whatever it naturally ended up being"
                instruction.writeC(cog, target + mask + 1 == 0);
                instruction.writeZ(cog, target + mask + 1 == 0);
                instruction.writeResult(cog, instruction.getDest(), target+mask+1);
            }
        }),
        WAITVID(0b111111, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                // TODO
            }
        }),
        XOR(0b011011, new BiConsumer<Cog, Instruction>() {
            @Override
            public void accept(Cog cog, Instruction instruction) {
                int source = instruction.getSourceValue(cog);
                int dest = instruction.getDestValue(cog);
                int result = source ^ dest;

                instruction.writeC(cog, getParity(result));
                instruction.writeZ(cog, result == 0);
                instruction.writeResult(cog, instruction.getDest(), result);
            }
        }.andThen(incPC));

        private final int instr;

        private final BiConsumer<Cog, Instruction> exec_fn;

        private final Predicate<Cog> executable;

        private OpCode(int instr, Predicate<Cog> executable, BiConsumer<Cog, Instruction> exec_fn) {
            this.instr = instr;
            this.exec_fn = exec_fn;
            this.executable = executable;
        }

        private OpCode(int instr, BiConsumer<Cog, Instruction> exec_fn) {
            this(instr, waitNPredicate(4), exec_fn);
        }

        private OpCode(int instr, Predicate<Cog> executable) {
            this(instr, executable, null);
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
		public static boolean getSignedCarry(int a, int b) {
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
		public static boolean getUnsignedCarry(int a, int b) {
			if (a < 0 && b < 0)
                return true;
            if (b < 0 && b >= -a)
                return true;
            if (a < 0 && b >= -a)
                return true;
			return false;
		}

        public static boolean getParity(int n) {
            int parity = 0;
            for (int i=0; i<32; i++)
                parity ^= (n >> i) & 1;
            return parity == 1;
        }

    }

    private static final Predicate<Cog> ifZ = c -> c.getZFlag();
    private static final Predicate<Cog> ifC = c -> c.getCFlag();

    /**
     * Do not make static! Non-static prevents conditionals from affecting one another
     */
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

    private int destination, source, encodedInstr;

    private final Predicate<Cog> NOPPredicate = waitNPredicate(4);

    public Instruction(int encoded) {
        int instr = (encoded >> (32 - 6)) & 0b111111;
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
     *
     * @param cog Cog to execute on
     */

    public boolean canExecute(Cog cog) {
        return opcode.isExecutable(cog);
    }

    /**
     * Called once per clock tick
     * Updates cog.pc when it's time to move on
     *
     * @param cog
     */
    public void execute(Cog cog) {
        if (!this.condition.testCond(cog)) {
            if (NOPPredicate.test(cog)) {
                incPC.accept(cog, this);
            }
        } else {
            if (this.canExecute(cog)) {
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
        cond = (this.condition.equals(Condition.IF_ALWAYS)) ? "" : this.condition.name();
        dest = register2string(destination, false);
        src = register2string(source, immediate);
        effects = (this.write_result) ? "wr" : "nr";
        effects += ((this.write_carry) ? ",wc" : "") + ((this.write_zero) ? ",wz" : "");

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

        return String.format(" %-13s %-7s %-6s %-6s %s", cond, opcode, dest, src, effects);
    }
}
