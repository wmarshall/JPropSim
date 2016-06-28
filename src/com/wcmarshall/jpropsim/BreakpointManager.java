package com.wcmarshall.jpropsim;

import java.util.ArrayList;
import java.util.function.Predicate;

public class BreakpointManager {

    private ArrayList<Breakpoint> breakpoints = new ArrayList<>();

    private class Breakpoint implements Predicate<Hub> {

        private int cogid;
        private int address;
        private boolean active;

        public Breakpoint(int cog, int addr) {
            super();
            cogid = cog;
            address = addr;
            active = true;
        }

        @Override
        public boolean test(Hub hub) {
            return active && hub.getCog(cogid).getPC() == address && hub.getCog(cogid).getInstruction().getCycles() == 0;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Breakpoint)) return false;
            Breakpoint other = (Breakpoint) o;
            return other.cogid == this.cogid && other.address == this.address;
        }

        public void toggle() {
            active = !active;
        }
    }

    public void toggleBreakpoint(int cog, int addr) {

        Breakpoint toggle = new Breakpoint(cog, addr);
        for (Breakpoint b : breakpoints) {
            if (b.equals(toggle)) {
                b.toggle();
                return;
            }
        }

        breakpoints.add(toggle);
    }

    public boolean breakNow(Hub hub) {
        for (Breakpoint b : breakpoints) {
            if (b.test(hub)) return true;
        }

        return false;
    }
}
