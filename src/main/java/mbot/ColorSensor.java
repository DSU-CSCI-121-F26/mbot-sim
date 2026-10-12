package mbot;

import mbot.internal.Session;

/**
 * A color sensor under the front of the robot. It tells you the color of the floor right
 * under it.
 *
 * <pre>
 * ColorSensor eye = new ColorSensor(robot);
 * if (eye.color().equals("red")) {
 *     robot.stop();
 * }
 * </pre>
 *
 * <p>The colors it can report are {@code "white"} (plain floor), {@code "black"} (tape),
 * {@code "red"}, {@code "green"}, {@code "blue"} and {@code "yellow"}.
 *
 * <p>The color sensor is a simulator part for now: the real mBot2's line sensor can see color,
 * but the robot's software does not report it yet.
 */
public class ColorSensor {

    private final Session session;

    /**
     * Plugs a color sensor into the bottom of a robot.
     *
     * @param robot the robot to plug it into. Make the robot first.
     */
    public ColorSensor(Chassis robot) {
        if (robot == null) {
            throw new IllegalArgumentException("A ColorSensor needs a robot to plug into, but it was given null. "
                    + "Make the robot first, then pass it in: new ColorSensor(robot)");
        }
        session = robot.session;
        session.attach("color");
    }

    /**
     * The color of the floor under the sensor.
     *
     * @return one of "white", "black", "red", "green", "blue", "yellow"
     */
    public String color() {
        return session.readColor();
    }
}
