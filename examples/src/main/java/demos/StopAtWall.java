package demos;

import mbot.*;

/** while + a sensor: drive until something is close, then stop. */
@MBotLab(world = "wall-ahead")
public class StopAtWall {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();

        bot.forward(50);
        while (bot.readUltrasonic() > 20) {
            // keep driving: the wheels are still on
        }
        bot.stop();
        System.out.println("Stopped " + bot.readUltrasonic() + " cm from the wall");
    }
}
