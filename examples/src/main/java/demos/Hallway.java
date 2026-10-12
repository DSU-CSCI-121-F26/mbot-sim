package demos;

import mbot.*;

/** if / else in a loop: drive up the hallway, turn at the end, stop at the goal. */
@MBotLab(world = "hallway")
public class Hallway {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();
        int turns = 0;

        while (turns < 2) {
            if (bot.readUltrasonic() > 25) {
                bot.straight(10);
            } else {
                bot.turnRight(90);
                turns++;
                System.out.println("Turn " + turns);
            }
        }
    }
}
