package com.wcmarshall.jpropsim.debugger;

import com.wcmarshall.jpropsim.Hub;

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
            return active && hub.getCog(cogid).getPC() == address && hub.getCog(cogid).isActive() && hub.getCog(cogid).getInstruction().getCycles() == 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Breakpoint that = (Breakpoint) o;

            if (cogid != that.cogid) return false;
            if (address != that.address) return false;
            return active == that.active;

        }

        @Override
        public int hashCode() {
            int result = cogid;
            result = 31 * result + address;
            result = 31 * result + (active ? 1 : 0);
            return result;
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
