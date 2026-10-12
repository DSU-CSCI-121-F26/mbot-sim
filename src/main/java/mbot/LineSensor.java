package mbot;

import mbot.internal.Session;

/**
 * The line sensor: four small light probes in a row under the front of the robot.
 * Each one can tell whether it is over dark tape. Use it to find and follow lines on the floor.
 *
 * <pre>
 *        far left   left   right   far right
 *   adds:    8        4      2         1
 * </pre>
 *
 * <pre>
 * Chassis base = new Chassis();
 * LineSensor floor = new LineSensor(base);
 * while (!floor.seesTape()) {
 *     base.straight(2);
 * }
 * </pre>
 */
public class LineSensor {

    private final Session session;

    /**
     * Plugs a line sensor into the bottom of a robot.
     *
     * @param robot the robot to plug it into. Make the robot first.
     */
    public LineSensor(Chassis robot) {
        if (robot == null) {
            throw new IllegalArgumentException("A LineSensor needs a robot to plug into, but it was given null. "
                    + "Make the Chassis first, then pass it in: new LineSensor(base)");
        }
        session = robot.session;
        session.attach("line");
    }

    /**
     * What all four probes see, as one number from 0 to 15.
     * 0 means no probe sees tape. 6 means the two middle ones do. 15 means all four do.
     *
     * @return a number from 0 to 15
     */
    public int status() {
        return session.readLineStatus("status()");
    }

    /**
     * Whether any probe is over tape.
     *
     * @return true if at least one probe sees tape
     */
    public boolean seesTape() {
        return session.readLineStatus("seesTape()") != 0;
    }
}
