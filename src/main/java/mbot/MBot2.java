package mbot;

/**
 * Your mBot2 robot, fully put together: a {@link Chassis} that comes with its
 * ultrasonic sensor and line sensor already plugged in.
 *
 * <p>To build your own robot, write a class that {@code extends MBot2}. It gets everything an
 * MBot2 can do, and you add the extra sensors it needs as fields:
 *
 * <pre>
 * public class Scout extends MBot2 {
 *     private Scanner scanner;
 *
 *     public Scout() {
 *         super();                      // build the MBot2 part first
 *         scanner = new Scanner(this);  // then plug in the extras
 *     }
 * }
 * </pre>
 *
 * <p>Make one at the start of {@code main}, then tell it what to do:
 *
 * <pre>
 * MBot2 bot = new MBot2();
 * bot.straight(30);      // drive forward 30 cm
 * bot.turnLeft(90);      // turn left 90 degrees
 * </pre>
 *
 * <p>The method names are the same ones the real robot uses, so code you write
 * for the simulator will drive your real mBot2 later in the course.
 *
 * <p>Distances are in centimetres (cm). Angles are in degrees.
 */
public class MBot2 extends Chassis {

    /** Turns on the robot. Make one of these at the start of {@code main}. */
    public MBot2() {
        super();
    }

    /**
     * Reads the ultrasonic sensor (the robot's two "eyes" on the front).
     *
     * @return the distance to the nearest thing in front of the robot, in centimetres.
     *         If nothing is in range, you get 300.
     */
    public double readUltrasonic() {
        return session.readUltrasonic("readUltrasonic()");
    }

    /**
     * Reads the line sensor under the front of the robot. It has four light probes
     * in a row. Each probe that is over dark tape adds to the number:
     *
     * <pre>
     *   far left = 8    left = 4    right = 2    far right = 1
     * </pre>
     *
     * So 0 means no probe sees tape, 6 means the two middle probes see it,
     * and 15 means all four do.
     *
     * @return a number from 0 to 15
     */
    public int readLineStatus() {
        return session.readLineStatus("readLineStatus()");
    }

    /**
     * The robot's mission. An MBot2's own run() does nothing. When you write your own robot
     * class that {@code extends MBot2}, <b>override</b> this method with what your robot should do:
     *
     * <pre>
     * public class Scout extends MBot2 {
     *     &#64;Override
     *     public void run() {
     *         straight(30);
     *     }
     * }
     * </pre>
     */
    public void run() {
        session.emptyRun();
    }
}
