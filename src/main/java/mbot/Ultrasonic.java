package mbot;

import mbot.internal.Session;

/**
 * The ultrasonic sensor: the two "eyes" on the front of the robot. It sends out a sound
 * too high to hear and times the echo, so it can tell how far away things are.
 * Use it to detect walls and objects in front of the robot.
 *
 * <pre>
 * Chassis base = new Chassis();
 * Ultrasonic eyes = new Ultrasonic(base);
 * if (eyes.distance() &lt; 20) {
 *     base.stop();
 * }
 * </pre>
 */
public class Ultrasonic {

    private final Session session;

    /**
     * Plugs an ultrasonic sensor into the front of a robot.
     *
     * @param robot the robot to plug it into. Make the robot first.
     */
    public Ultrasonic(Chassis robot) {
        if (robot == null) {
            throw new IllegalArgumentException("An Ultrasonic needs a robot to plug into, but it was given null. "
                    + "Make the Chassis first, then pass it in: new Ultrasonic(base)");
        }
        session = robot.session;
        session.attach("ultrasonic");
    }

    /**
     * How far away the nearest thing in front of the robot is.
     *
     * @return the distance in centimetres. If nothing is in range, you get 300.
     */
    public double distance() {
        return session.readUltrasonic("distance()");
    }
}
