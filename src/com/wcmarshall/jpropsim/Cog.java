package com.wcmarshall.jpropsim;

import com.wcmarshall.jpropsim.disassembler.Instruction;

public class Cog {

    public static final int PAR_ADDR = 0x1F0;
    public static final int CNT_ADDR = 0x1F1;
    public static final int INA_ADDR = 0x1F2;
    public static final int INB_ADDR = 0x1F3;
    public static final int OUTA_ADDR = 0x1F4;
    public static final int OUTB_ADDR = 0x1F5;
    public static final int DIRA_ADDR = 0x1F6;
    public static final int DIRB_ADDR = 0x1F7;
    public static final int CTRA_ADDR = 0x1F8;
    public static final int CTRB_ADDR = 0x1F9;
    public static final int FRQA_ADDR = 0x1FA;
    public static final int FRQB_ADDR = 0x1FB;
    public static final int PHSA_ADDR = 0x1FC;
    public static final int PHSB_ADDR = 0x1FD;
    public static final int VCFG_ADDR = 0x1FE;
    public static final int VSCL_ADDR = 0x1FF;

    private final Hub hub;

    private final int id;

    private boolean running = false, prog_loaded = false;
    private int hub_prog_addr = 0, prog_load_count = 0;

    private int pc = 0;

    private int[] cogram = new int[512];

    private boolean zflag = false, cflag = false;

    private Instruction current, next;

    private Counter counterA, counterB;

    public Cog(Hub hub, int id) {
        this.id = id;
        this.hub = hub;
        this.counterA = new Counter(this);
        this.counterB = new Counter(this);
    }

    public Instruction getInstruction() {
        return current;
    }

    public int getID() {
        return id;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(int n) {
        pc = n;
        current = new Instruction(this, cogram[pc]);
        next = new Instruction(this, cogram[pc+1]);
    }

    public void incrementPC() {
        pc++;
        current = next;
        next = new Instruction(this, cogram[pc+1]);
    }

    public int[] getCogram() {
        return cogram.clone();
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
        switch (addr) {
            case CNT_ADDR:
                return hub.getCnt();
            case INA_ADDR:
                return hub.getIna();
            case INB_ADDR:
                // unimplemented on P8X32A
                return 0;
            default:
                return cogram[addr];
        }
    }

    public void setLong(int addr, int value) {
        switch (addr) {
            case PAR_ADDR:
            case CNT_ADDR:
            case INA_ADDR:
            case INB_ADDR:
                return;
            default:
                cogram[addr] = value;
        }
    }

    public void setPinOut(int pin, boolean state) {
        if (pin > 31) return;

        if (state) {
            setLong(OUTA_ADDR, getLong(OUTA_ADDR) | (1 << pin));
        } else {
            setLong(OUTA_ADDR, getLong(OUTA_ADDR) & ~(1 << pin));
        }
    }

    public boolean getPinIn(int pin) {
        if (pin > 31) return false;
        return ((getLong(INA_ADDR) >> pin) & 1) == 1;
    }

    public Hub getHub() {
        return hub;
    }

    public int getINA() {
        return getLong(INA_ADDR);
    }

    public void start(int hub_prog_addr, int arg) {
        for (int i = 0; i < 0x1ff; i++) {
            setLong(i, 0);
        }
        this.running = true;
        this.prog_loaded = false;
        this.hub_prog_addr = hub_prog_addr;
        this.prog_load_count = 0;
        this.setCFlag(false);
        this.setZFlag(false);
        cogram[PAR_ADDR] = arg;
    }

    public void stop() {
        running = false;
    }

    public boolean isHubAligned() {
        return hub.isAligned(this);
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isActive() {
        return isRunning() && prog_loaded;
    }

    public int getCnt() {
        return hub.getCnt();
    }

    public void tick() {
        if (running) {
            if (prog_loaded) {
                current.execute();
                // update counters
                setLong(PHSA_ADDR, counterA.tick(getLong(CTRA_ADDR), getLong(FRQA_ADDR), getLong(PHSA_ADDR)));
                setLong(PHSB_ADDR, counterB.tick(getLong(CTRB_ADDR), getLong(FRQB_ADDR), getLong(PHSB_ADDR)));
            } else {
                if (isHubAligned()) {
                    this.setLong(prog_load_count, hub.getLong(this.hub_prog_addr + 4 * prog_load_count));
                    prog_load_count++;
                    if (prog_load_count > 0x1ef) {
                        prog_loaded = true;
                        setPC(0);
                    }
                }
            }
        }
    }
}
