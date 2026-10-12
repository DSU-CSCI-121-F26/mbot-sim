package mbot;

import mbot.internal.Session;

/**
 * A bare mBot2: two wheels with motors, and the CyberPi controller on top with its five lights.
 * <b>No sensors.</b> To let it see, plug sensors into it:
 *
 * <pre>
 * Chassis base = new Chassis();
 * Ultrasonic eyes = new Ultrasonic(base);     // now it can see things in front of it
 * LineSensor floor = new LineSensor(base);    // now it can see tape on the floor
 * </pre>
 *
 * <p>If you want the whole robot already put together, use {@link MBot2} instead.
 *
 * <p>Distances are in centimetres (cm). Angles are in degrees.
 */
public class Chassis {

    final Session session;

    /** Turns on a bare robot: motors and lights, no sensors yet. */
    public Chassis() {
        this(false);
    }

    /** For MBot2, which comes with its sensors already plugged in. */
    Chassis(boolean withSensors) {
        session = Session.start(withSensors ? "MBot2" : "Chassis");
    }

    // ------------------------------------------------------------------ driving

    /**
     * Drives straight and waits until it gets there.
     * A negative distance drives backward.
     *
     * @param distanceInCm how far to drive, in centimetres
     */
    public void straight(double distanceInCm) {
        session.straight(distanceInCm);
    }

    /**
     * Turns left on the spot and waits until the turn is done.
     *
     * @param degrees how far to turn, in degrees (90 is a quarter turn)
     */
    public void turnLeft(double degrees) {
        session.turn(degrees, "turnLeft");
    }

    /**
     * Turns right on the spot and waits until the turn is done.
     *
     * @param degrees how far to turn, in degrees (90 is a quarter turn)
     */
    public void turnRight(double degrees) {
        session.turn(-degrees, "turnRight");
    }

    /**
     * Starts both wheels driving forward and keeps going.
     * This does <b>not</b> wait: your next line of code runs right away
     * while the robot is still moving. Call {@link #stop()} to stop.
     *
     * @param speed wheel speed, from 0 to 200 (try 50)
     */
    public void forward(double speed) {
        session.forward(speed, -1, "forward(" + Session.num(speed) + ")");
    }

    /**
     * Drives forward for a number of seconds, then stops.
     * This waits until the time is up.
     *
     * @param speed wheel speed, from 0 to 200 (try 50)
     * @param seconds how long to drive
     */
    public void forward(double speed, double seconds) {
        session.forward(speed, seconds, "forward(" + Session.num(speed) + ", " + Session.num(seconds) + ")");
    }

    /**
     * Sets each wheel's power separately and keeps going. This does not wait.
     * Equal powers drive straight. Different powers curve. Opposite powers spin.
     *
     * @param leftPower left wheel power, from -100 to 100
     * @param rightPower right wheel power, from -100 to 100
     */
    public void setMotorPower(double leftPower, double rightPower) {
        session.motorPower(leftPower, rightPower);
    }

    /** Stops both wheels. */
    public void stop() {
        session.stop();
    }

    // ------------------------------------------------------------------ lights

    /**
     * Turns on one of the five lights on the CyberPi.
     *
     * @param id which light, from 1 to 5
     * @param red how much red, from 0 to 255
     * @param green how much green, from 0 to 255
     * @param blue how much blue, from 0 to 255
     */
    public void turnLedOn(int id, int red, int green, int blue) {
        session.led(id, red, green, blue);
    }

    /**
     * Turns off one of the five lights on the CyberPi.
     *
     * @param id which light, from 1 to 5
     */
    public void turnLedOff(int id) {
        session.led(id, 0, 0, 0);
    }
}
