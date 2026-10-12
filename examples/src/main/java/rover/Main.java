package rover;

import mbot.*;

/** Classroom demo of composition: a Rover that owns a chassis and a sensor. */
@MBotLab(world = "wall-ahead")
public class Main {
    public static void main(String[] args) {
        Rover scout = new Rover("Scout");
        System.out.println(scout + " is ready");

        scout.driveUntilBlocked();
        System.out.println(scout + " stopped before the wall");
    }
}
