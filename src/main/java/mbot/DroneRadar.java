package mbot;

import mbot.internal.Session;

/**
 * A radar that picks up the drones' signals, even through walls. Use it to play hide and seek:
 * know when you are being watched, and which way the nearest drone is.
 *
 * <pre>
 * DroneRadar radar = new DroneRadar(robot);
 * if (radar.watched()) {
 *     robot.stop();          // freeze, or get behind a wall
 * }
 * </pre>
 *
 * <p>The radar is a simulator part: there are no drones on the real robot.
 */
public class DroneRadar {

    private final Session session;

    /**
     * Plugs a drone radar into a robot.
     *
     * @param robot the robot to plug it into. Make the robot first.
     */
    public DroneRadar(Chassis robot) {
        if (robot == null) {
            throw new IllegalArgumentException("A DroneRadar needs a robot to plug into, but it was given null. "
                    + "Make the robot first, then pass it in: new DroneRadar(robot)");
        }
        session = robot.session;
        session.attach("radar");
    }

    /** @return true if any drone can see the robot right now. Stay in sight too long and you get tagged. */
    public boolean watched() {
        return session.radar("watched()") == 1;
    }

    /** @return how far away the nearest drone is, in cm, or 300 if there are no drones */
    public double nearestDistance() {
        return session.radar("nearestDistance()");
    }

    /**
     * How far the nearest drone is looking away from the robot, in degrees. 0 means it faces
     * the robot (it would see it, if no wall were in the way); 180 means it faces straight away.
     * Drones see 30° to each side, so above about 40 it is not looking your way.
     * Hiding behind a wall? Wait for a big number, then go.
     *
     * @return from 0 to 180, or 180 if there are no drones
     */
    public double nearestLookAngle() {
        return session.radar("nearestLookAngle()");
    }

    /**
     * Which way the nearest drone is, compared to where the robot faces. Positive is to the left.
     *
     * @return the angle in degrees, from -180 to 180, or 0 if there are no drones
     */
    public double nearestAngle() {
        return session.radar("nearestAngle()");
    }
}
