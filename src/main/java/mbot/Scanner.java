package mbot;

import mbot.internal.Session;

/**
 * A scanner: an ultrasonic sensor on top of a servo motor, so it can turn and look in
 * different directions without the robot turning.
 *
 * <pre>
 * Scanner scanner = new Scanner(robot);
 * double[] d = scanner.scan();    // 7 distances, from far left to far right
 * double ahead = d[3];            // the middle one is straight ahead
 * </pre>
 *
 * <p>The scanner is a simulator part: the real mBot2 has the servo port, but the robot's
 * software cannot drive a scanner yet.
 */
public class Scanner {

    /**
     * The angles {@link #scan()} looks at, in the same order as the distances it returns.
     * Positive is to the robot's left: 90 = far left, 0 = straight ahead, -90 = far right.
     */
    public static final int[] ANGLES = {90, 60, 30, 0, -30, -60, -90};

    private final Session session;

    /**
     * Mounts a scanner on top of a robot.
     *
     * @param robot the robot to mount it on. Make the robot first.
     */
    public Scanner(Chassis robot) {
        if (robot == null) {
            throw new IllegalArgumentException("A Scanner needs a robot to mount on, but it was given null. "
                    + "Make the robot first, then pass it in: new Scanner(robot)");
        }
        session = robot.session;
        session.attach("scanner");
    }

    /**
     * Turns the scanner to 7 angles and measures the distance at each one.
     *
     * @return 7 distances in cm, from far left (index 0) to far right (index 6).
     *         Index 3 is straight ahead. 300 means nothing is in range.
     * @see #ANGLES
     */
    public double[] scan() {
        return session.scan(ANGLES);
    }

    /**
     * Turns the scanner to one angle and measures the distance there.
     *
     * @param degrees which way to look: positive is left, negative is right, 0 is straight ahead.
     *                From -90 to 90.
     * @return the distance in cm, or 300 if nothing is in range
     */
    public double look(int degrees) {
        return session.look(degrees);
    }
}
