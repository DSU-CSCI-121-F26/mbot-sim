package mbot;

/**
 * A drone that stays in one place and spins slowly, like a lighthouse.
 * Watch which way it faces, and move while it looks away.
 */
public class Sentry extends Drone {

    private final double spin;

    /** A sentry that turns 45 degrees a second to the left. */
    public Sentry() {
        this(45);
    }

    /**
     * A sentry that spins at its own speed.
     *
     * @param degreesPerSecond positive spins left, negative spins right
     */
    public Sentry(double degreesPerSecond) {
        spin = degreesPerSecond;
    }

    @Override
    public void tick() {
        setSpeed(0);
        setTurnRate(spin);
    }
}
