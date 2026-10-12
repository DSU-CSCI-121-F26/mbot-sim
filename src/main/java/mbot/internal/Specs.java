package mbot.internal;

/**
 * The robot's physical numbers, in cm, seconds and degrees.
 *
 * Anything marked UNCALIBRATED is a plausible placeholder, not a measurement of a real
 * mBot2. The BRACE sources have no cm/s figure (straight() bottoms out in vendor firmware),
 * so these need someone with a robot, a tape measure and a stopwatch. StingSim's
 * config.json has the same caveat.
 */
public final class Specs {

    private Specs() {}

    /** Body treated as a circle for collisions. mBot2 is about 17 x 13 cm. */
    public static final double RADIUS_CM = 8.5;

    /** Distance between the two drive wheels. UNCALIBRATED. */
    public static final double TRACK_CM = 11.5;

    /** Tyre diameter. UNCALIBRATED (mBot2 tyres are about 6.5 cm). */
    public static final double WHEEL_DIAMETER_CM = 6.5;

    /** forward(speed) takes encoder-motor RPM; mBot2's motors top out near 200. */
    public static final double MAX_RPM = 200;

    /** straight() and turnLeft()/turnRight() speeds. UNCALIBRATED. */
    public static final double STRAIGHT_CM_PER_S = 15;
    public static final double TURN_DEG_PER_S = 90;

    /** Ultrasonic sensor: how far ahead of the centre it sits, and its cone. */
    public static final double US_OFFSET_CM = 7;
    public static final double US_HALF_ANGLE_DEG = 15;
    public static final double US_MAX_CM = 300;
    public static final double US_MIN_CM = 3;

    /** Scanner: an ultrasonic on a servo, mounted on top just ahead of the centre. Simulator-only part. */
    public static final double SCANNER_OFFSET_CM = 3;
    public static final double SCANNER_HALF_ANGLE_DEG = 8;
    public static final double SCAN_STEP_S = 0.15;

    /** Quad line sensor: probes sit this far ahead of the centre, at these sideways offsets (left is +). */
    public static final double LINE_AHEAD_CM = 6;
    public static final double[] LINE_PROBES_CM = {3.6, 1.2, -1.2, -3.6};

    /** Each command round trip to the real robot takes time; a sensor read is never free. */
    public static final double READ_TICK_S = 0.02;

    /** Stop a runaway program after this much robot time, or this many recorded steps. */
    public static final double MAX_SIM_SECONDS = 600;
    public static final int MAX_FRAMES = 20_000;

    public static double rpmToCmPerS(double rpm) {
        return rpm / 60.0 * Math.PI * WHEEL_DIAMETER_CM;
    }
}
