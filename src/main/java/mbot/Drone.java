package mbot;

import mbot.internal.DroneBody;

/**
 * A drone that hovers over the board, looking for robots. If it keeps a robot in sight for
 * half a second, it <b>tags</b> it. A robot that is tagged too many times shuts down.
 *
 * <p>To make your own drone, extend this class and write {@link #tick()}. The simulator calls
 * {@code tick()} ten times a second. Each time, look at what the drone can see and decide
 * what to do next, usually with a mode (an {@code enum}) and a {@code switch}:
 *
 * <pre>
 * public class Hawk extends Drone {
 *     private enum Mode { SEARCH, CHASE }
 *     private Mode mode = Mode.SEARCH;
 *
 *     &#64;Override
 *     public void tick() {
 *         switch (mode) {
 *             case SEARCH -&gt; {
 *                 setSpeed(0);
 *                 setTurnRate(60);              // spin and look around
 *                 if (seesRobot()) mode = Mode.CHASE;
 *             }
 *             case CHASE -&gt; {
 *                 setTurnRate(robotAngle() * 3);   // steer toward the robot
 *                 setSpeed(MAX_SPEED);
 *                 if (!seesRobot()) mode = Mode.SEARCH;
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>Drones cannot see through walls or boxes, so a robot can hide behind them.
 * Drones fly over cans, but not over walls or boxes.
 */
public abstract class Drone {

    /** How far a drone can see, in cm. */
    public static final double VISION_RANGE = 120;

    /** Half the width of a drone's view: it sees 30° to each side of where it faces. */
    public static final double VISION_HALF_ANGLE = 30;

    /** The fastest a drone can fly, in cm per second. A robot driving straight goes 15. */
    public static final double MAX_SPEED = 12;

    /** The fastest a drone can turn, in degrees per second. */
    public static final double MAX_TURN_RATE = 120;

    private DroneBody body;

    static {
        DroneBody.bridge(new DroneBody.Bridge() {
            @Override
            public void bind(Drone d, DroneBody b) {
                d.body = b;
            }
        });
    }

    /**
     * Decide what to do next. Called ten times a second while the robot's program runs.
     * Use {@link #setSpeed} and {@link #setTurnRate} to move; they keep going until you change them.
     */
    public abstract void tick();

    /**
     * How fast to fly forward. It keeps flying at this speed until you change it.
     *
     * @param cmPerSecond from -12 to 12 ({@link #MAX_SPEED}). 0 hovers in place.
     */
    protected final void setSpeed(double cmPerSecond) {
        body.speed = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, cmPerSecond));
    }

    /**
     * How fast to turn. Positive turns left, negative turns right. It keeps turning until
     * you change it.
     *
     * @param degreesPerSecond from -120 to 120 ({@link #MAX_TURN_RATE}). 0 flies straight.
     */
    protected final void setTurnRate(double degreesPerSecond) {
        body.turnRate = Math.max(-MAX_TURN_RATE, Math.min(MAX_TURN_RATE, degreesPerSecond));
    }

    /** @return true if the robot is in view: close enough, in front, and not behind a wall or box */
    protected final boolean seesRobot() {
        return body.seeing;
    }

    /**
     * Which way the robot is from where this drone faces. Positive means it is to the left.
     *
     * @return the angle in degrees, from -180 to 180, or NaN if the drone cannot see the robot
     */
    protected final double robotAngle() {
        return body.seeing ? body.robotAngle : Double.NaN;
    }

    /** @return how far away the robot is in cm, or NaN if the drone cannot see the robot */
    protected final double robotDistance() {
        return body.seeing ? body.robotDistance : Double.NaN;
    }

    /** @return how far the drone can fly straight ahead before it reaches a wall or box, in cm */
    protected final double wallAhead() {
        return body.wallAhead();
    }

    /** @return this drone's position, in cm */
    protected final double x() {
        return body.x;
    }

    /** @return this drone's position, in cm */
    protected final double y() {
        return body.y;
    }

    /** @return which way this drone faces, 0 to 360 degrees (90 = up the board) */
    protected final double heading() {
        return body.h;
    }

    /** @return the board's width (x direction), in cm */
    protected final double boardWidth() {
        return body.boardWidth();
    }

    /** @return the board's depth (y direction), in cm */
    protected final double boardHeight() {
        return body.boardHeight();
    }

    /**
     * Random numbers for this drone. They come out the same every run, so a replay or a test
     * always does the same thing. Example: {@code random().nextDouble() * boardWidth()}.
     */
    protected final java.util.Random random() {
        return body.random;
    }

    /** @return seconds since the robot's program started */
    protected final double time() {
        return body.time();
    }

    /** @return this drone's name, for the viewer. Override it to choose your own. */
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
