package hideandseek;

import mbot.*;

/** A robot that carries a drone radar, and freezes behind cover when it is spotted. */
public class Sneak extends MBot2 {
    private DroneRadar radar;

    public Sneak() {
        super("Sneak", "black", "green");
        radar = new DroneRadar(this);
    }

    /** Drive in short hops; if a drone sees us, back up 10 cm and wait a moment. */
    public void sneak(double cm) {
        double done = 0;
        while (done < cm) {
            if (radar.watched()) {
                straight(-10);
                done -= 10;
                for (int i = 0; i < 40; i++) readUltrasonic();    // wait about 0.8 s
            } else {
                straight(10);
                done += 10;
            }
        }
    }

    @Override
    public void run() {
        sneak(140);          // up the left side, to the top row
        turnRight(90);
        sneak(230);          // along the top, behind the cover wall
    }
}
