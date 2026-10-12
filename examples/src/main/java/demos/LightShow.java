package demos;

import mbot.*;

/** Nested loops and the five CyberPi lights. */
@MBotLab(world = "open-room")
public class LightShow {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();

        for (int lap = 0; lap < 3; lap++) {
            for (int id = 1; id <= 5; id++) {
                bot.turnLedOn(id, 255, 40 * id, 0);
            }
            bot.turnRight(120);
            for (int id = 1; id <= 5; id++) {
                bot.turnLedOff(id);
            }
        }
    }
}
