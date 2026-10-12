package demos;

import mbot.*;

/** The line sensor: creep forward until the probes see the tape of the parking box. */
@MBotLab(world = "parking")
public class FindTheBox {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();

        int steps = 0;
        while (bot.readLineStatus() == 0) {
            bot.straight(5);
            steps++;
        }
        System.out.println("Found the tape after " + steps + " moves");
        bot.straight(30);           // roll into the middle of the box
        bot.turnLedOn(3, 0, 200, 0);
    }
}
