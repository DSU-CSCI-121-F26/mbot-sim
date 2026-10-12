package mbot;

import mbot.internal.Session;

/**
 * Your mBot2 robot.
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
public class MBot2 {

    private final Session session;

    /** Turns on the robot. Make one of these at the start of {@code main}. */
    public MBot2() {
        session = Session.start();
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

    // ------------------------------------------------------------------ sensors

    /**
     * Reads the ultrasonic sensor (the robot's two "eyes" on the front).
     *
     * @return the distance to the nearest thing in front of the robot, in centimetres.
     *         If nothing is in range, you get 300.
     */
    public double readUltrasonic() {
        return session.readUltrasonic();
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
        return session.readLineStatus();
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
