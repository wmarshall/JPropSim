package com.wcmarshall.jpropsim;

public class Counter {

    private Cog cog;

    public Counter(Cog c) {
        cog = c;
    }

    /**
     *
     * Updates the counter. Since the counter is not aware of whether it is counter A or B
     * all registers are passed to it, and the updated phase is returned. Any pins read or set
     * by the counter are specified within the CTRx register, and will therefore be handled
     * within the Counter object.
     *
     * @param ctr Control register for counter
     * @param frq Frequency register for counter
     * @param phs Phase register counter
     * @return updated phase register
     */
    public int tick(int ctr, int frq, int phs) {

        int mode = (ctr >> 26) & 0b11111;
        int plldiv = (ctr >> 23) & 0b111;
        int pina = (ctr >> 0) & 0b111111;
        int pinb = (ctr >> 9) & 0b111111;

        switch (mode) {
            case 0b00000: // counter disabled
                break;
            case 0b00001: // PLL internal video mode
            case 0b00010: // PLL single ended
            case 0b00011: // PLL differential
            case 0b00100: // NCO single ended
                phs += frq;
                cog.setPinOut(pina, (phs >>> 31) == 1);
                break;
            case 0b00101: // NCO differential
                phs += frq;
                cog.setPinOut(pina, (phs >>> 31) == 1);
                cog.setPinOut(pina, (phs >>> 31) == 0);
                break;
            case 0b00110: // duty single ended
                cog.setPinOut(pina, getUnsignedCarry(phs, frq));
                phs += frq;
                break;
            case 0b00111: // duty differential
                cog.setPinOut(pina, getUnsignedCarry(phs, frq));
                cog.setPinOut(pina, !getUnsignedCarry(phs, frq));
                phs += frq;
                break;
            case 0b01000: // positive
            case 0b01001: // positive w/feedback
            case 0b01010: // positive edge
            case 0b01011: // positive edge w/feedback
            case 0b01100: // negative
            case 0b01101: // negative w/feedback
            case 0b01110: // negative edge
            case 0b01111: // negative edge w/feedback
            case 0b10000: // never
                break;
            case 0b10001: // !A & !B
            case 0b10010: // A & !B
            case 0b10011: // !B
            case 0b10100: // !A & B
            case 0b10101: // !A
            case 0b10110: // A != B
            case 0b10111: // !A | !B
            case 0b11000: // A & B
            case 0b11001: // A == B
            case 0b11010: // A
            case 0b11011: // A | !B
            case 0b11100: // B
            case 0b11101: // !A | B
            case 0b11110: // A | B
            case 0b11111: // always
                phs += frq;
        }

        return phs;
    }

    /**
     * Calculates the carry bit resulting from the addition of two unsigned integers a & b
     * This assumes that the formula is a+b. To calculate the carry from a-b, pass -a and b
     *
     * @param a first element of addition
     * @param b second element of addition
     * @return returns whether or not a carry bit is generated from a+b
     */
    private boolean getUnsignedCarry(int a, int b) {
        if (a < 0 && b < 0)
            return true;
        if (b < 0 && b >= -a)
            return true;
        if (a < 0 && b >= -a)
            return true;
        return false;
    }
}
