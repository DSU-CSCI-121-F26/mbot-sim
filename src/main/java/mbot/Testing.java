package mbot;

import mbot.internal.Session;

import java.util.Set;

/**
 * For JUnit tests in labs: run student code in a world with no viewer, then check where the
 * robot ended up.
 *
 * <pre>
 * &#64;Test
 * void parksInTheBox() {
 *     Testing.start("parking");
 *     new Rover().park();
 *     assertTrue(Testing.insideZone("P"));
 *     assertEquals(0, Testing.bumps());
 * }
 * </pre>
 *
 * Each {@link #start} begins a fresh, headless run: the next {@code new Chassis()} or
 * {@code new MBot2()} joins it. Nothing is saved and no browser opens.
 */
public final class Testing {

    private Testing() {}

    /** Begin a fresh headless run in a built-in world (or a .json path). */
    public static void start(String world) {
        Session.startTesting(world);
    }

    /** Begin a fresh headless run on a board you built. */
    public static void start(Class<? extends Board> board) {
        Session.startTesting(board);
    }

    /** Begin a fresh headless run on a board object. */
    public static void start(Board board) {
        Session.startTesting(board);
    }

    /**
     * Run some robot code, and stop quietly if the robot shuts down (tagged out, or out of time),
     * the way a real run would. Check {@link #passed()} afterwards.
     */
    public static void run(Runnable mission) {
        try {
            mission.run();
        } catch (RobotShutDownException e) {
            // the robot is off; the test looks at the result
        }
    }

    /** How many times drones have tagged the robot. */
    public static int tags() {
        return Session.testing().tags();
    }

    /** True if the robot has shut down (too many tags, or out of time). */
    public static boolean shutDown() {
        return Session.testing().shutdown() != null;
    }

    /**
     * Did the robot pass the board? It must end in the Goal (if there is one), stay within the
     * bump limit (if there is one), and not shut down.
     */
    public static boolean passed() {
        return Session.testing().passed();
    }

    /** Why the board was not passed, in words. Empty if it was. */
    public static java.util.List<String> why() {
        return Session.testing().why();
    }

    /** Robot centre, in cm. */
    public static double x() {
        return Session.testing().x();
    }

    /** Robot centre, in cm. */
    public static double y() {
        return Session.testing().y();
    }

    /** Which way the robot faces, 0 to 360 degrees. 90 = away from the start, in most worlds. */
    public static double heading() {
        return Session.testing().heading();
    }

    /** How many times the robot ran into a wall or an object. */
    public static int bumps() {
        return Session.testing().bumps();
    }

    /** Whether the robot's centre is inside the zone with this label in the world. */
    public static boolean insideZone(String label) {
        return Session.testing().insideZone(label);
    }

    /** Which sensors are plugged in: "ultrasonic", "line". */
    public static Set<String> parts() {
        return Session.testing().parts();
    }

    /** Robot seconds used so far. */
    public static double time() {
        return Session.testing().time();
    }

    /** How many times any sensor was read. */
    public static int sensorReads() {
        return Session.testing().reads();
    }
}
