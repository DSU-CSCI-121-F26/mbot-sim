package rover;

import mbot.*;

/** Composition: a Rover HAS-A chassis and HAS-AN ultrasonic sensor. */
public class Rover {
    private Chassis base;
    private Ultrasonic eyes;
    private String name;

    public Rover(String name) {
        this.name = name;
        this.base = new Chassis();
        this.eyes = new Ultrasonic(base);
    }

    public boolean somethingAhead(double cm) {
        return eyes.distance() < cm;
    }

    public void driveUntilBlocked() {
        while (!somethingAhead(15)) {
            base.straight(5);
        }
    }

    public String toString() {
        return name + " the rover";
    }
}
