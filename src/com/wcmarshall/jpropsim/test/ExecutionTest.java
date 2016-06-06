package com.wcmarshall.jpropsim.test;

import com.wcmarshall.jpropsim.Cog;
import com.wcmarshall.jpropsim.Hub;
import com.wcmarshall.jpropsim.disassembler.Disassembler;

import java.io.IOException;
import java.util.Scanner;

public class ExecutionTest {

    public static void main(String[] args) {

        Scanner user = new Scanner(System.in);
        Disassembler disasm = new Disassembler();
        boolean running = true;
        Hub hub = null;

        try {
            hub = new Hub();
        } catch (IOException e) {
            System.out.println("Unable to open ROM file");
            user.close();
            return;
        }

        while (running) {

            System.out.print("\nCMD: ");
            String cmd = user.next();

            if (cmd.equalsIgnoreCase("quit")) {
                running = false;
            } else if (cmd.equalsIgnoreCase("step")) {
                int target = 1;
                if (user.hasNextInt()) {
                    target = user.nextInt();
                    if (target < 0) target = 1;
                }

                for (int i=0; i<target; i++)
                    hub.tick();

            } else if (cmd.equalsIgnoreCase("status")) {
                if (user.hasNextInt()) {
                    int cog = user.nextInt();
                    if (cog < 0 || cog > 7) {
                        System.out.println("\nCog must be in range [0, 7]");
                        continue;
                    }

                    Cog c = hub.getCog(cog);
                    System.out.printf("\nCOGID:  %d\nSTATUS: %s\nPC:     %04X\n",
                            c.getID(), (c.isRunning()) ? "Running" : "Stopped", c.getPC());
                } else {
                    System.out.printf("\nCNT: %08X\nHUB: %d\nCOGS:\n", hub.getCnt(), hub.getAlignment());
                    for (int i=0; i<8; i++) {
                        Cog c = hub.getCog(i);
                        System.out.printf("COGID: %d STATUS: %s PC: %04X\n",
                                c.getID(), (c.isRunning()) ? "Running" : "Stopped", c.getPC());
                    }
                }

            } else if (cmd.equalsIgnoreCase("list")) {
                System.out.print("\nCOG NUMBER: ");
                int cog = user.nextInt();
                if (cog < 0 || cog > 7) {
                    System.out.println("\nCog must be in range [0, 7]");
                    continue;
                }

                Cog c = hub.getCog(cog);
                System.out.printf("\nCOGID:  %d\nSTATUS: %s\nPC:     %04X\n",
                        c.getID(), (c.isRunning()) ? "Running" : "Stopped", c.getPC());
                System.out.println(disasm.generateListing(c.getCogram()));
            } else if (cmd.equalsIgnoreCase("listio")) {
                String ina = String.format("%32s", Integer.toBinaryString(hub.getIna())).replace(' ', '0');
                String outa = String.format("%32s", Integer.toBinaryString(hub.getOuta())).replace(' ', '0');
                String dira = String.format("%32s", Integer.toBinaryString(hub.getDira())).replace(' ', '0');
                System.out.printf("Effective DIRA: %s\nEffective INA:  %s\nEffective OUTA: %s", dira, ina, outa);
            } else if (cmd.equalsIgnoreCase("setpin")) {
                System.out.print("PIN NUMBER: ");
                int pin = user.nextInt();
                if (pin < 0 || pin > 31) {
                    System.out.println("\nPin must be in range [0,31]");
                    continue;
                }

                System.out.print("\nSTATE {0,1}: ");
                boolean state = user.nextInt() == 1;
                hub.setPinIn(pin, state);
            } else {
                System.out.println("Unknown command");
            }
        }
    }
}
