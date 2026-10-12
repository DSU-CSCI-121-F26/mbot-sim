package mbot.internal;

import mbot.Drone;

import java.util.ArrayList;
import java.util.List;

/**
 * A drone's physical state in the simulator. The student's {@link Drone} object holds one and
 * reads and steers it through Drone's protected methods.
 *
 * Drones hover: they are blocked by walls and boxes (and cannot see through them), but fly
 * over cans and never collide with the robot.
 */
public final class DroneBody {

    /** Lets Drone hand its private field to the simulator without making it public. */
    public interface Bridge {
        void bind(Drone d, DroneBody b);
    }

    private static Bridge bridge;

    /** Called once, from Drone's static initializer. */
    public static void bridge(Bridge b) {
        bridge = b;
    }

    static final double RADIUS_CM = 6;
    static final double TAG_AFTER_S = 0.5;      // in sight this long = tagged
    static final double COOLDOWN_S = 2;         // then it cannot tag again for this long

    final Drone drone;
    final String name;
    final Sim sim;
    public double x, y, h;
    public double speed, turnRate;
    public boolean seeing;
    public double robotAngle, robotDistance;
    double watch, cooldown;
    final List<double[]> samples = new ArrayList<>();   // {t, x, y, h, seeing 0/1}, drained per frame
    public final java.util.Random random;

    DroneBody(Drone drone, String name, double x, double y, double h, Sim sim) {
        this.drone = drone;
        this.name = name;
        this.x = x;
        this.y = y;
        this.h = h;
        this.sim = sim;
        // Same seed every run (from the name and start), so replays and tests repeat exactly
        this.random = new java.util.Random(name.hashCode() * 31L + Math.round(x) * 1009L + Math.round(y));
        bridge.bind(drone, this);
    }

    public double boardWidth() {
        return sim.world.width;
    }

    public double boardHeight() {
        return sim.world.height;
    }

    public double time() {
        return sim.time;
    }

    /** Fly for dt seconds at the current speed and turn rate. Walls and boxes stop it. */
    void move(double dt) {
        h = Sim.norm(h + turnRate * dt);
        double rad = Math.toRadians(h);
        double nx = x + speed * dt * Math.cos(rad), ny = y + speed * dt * Math.sin(rad);
        if (!blocked(nx, ny)) {
            x = nx;
            y = ny;
        }
    }

    boolean blocked(double cx, double cy) {
        for (double[] b : sim.world.boxes) {
            double nx = Sim.clamp(cx, b[0], b[0] + b[2]), ny = Sim.clamp(cy, b[1], b[1] + b[3]);
            double dx = cx - nx, dy = cy - ny;
            if (dx * dx + dy * dy < RADIUS_CM * RADIUS_CM) return true;
        }
        return false;
    }

    /** Can this drone see the robot right now? In range, inside the cone, nothing solid between. */
    void look() {
        double dx = sim.x - x, dy = sim.y - y, dist = Math.hypot(dx, dy);
        double angle = angleTo(Math.toDegrees(Math.atan2(dy, dx)) - h);
        robotDistance = dist;
        robotAngle = angle;
        seeing = dist <= Drone.VISION_RANGE && Math.abs(angle) <= Drone.VISION_HALF_ANGLE
                && clearLine(x, y, sim.x, sim.y, sim.world);
    }

    /** True if nothing solid (walls, boxes) is between the two points. */
    static boolean clearLine(double ax, double ay, double bx, double by, World w) {
        double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy);
        if (len < 1e-9) return true;
        dx /= len;
        dy /= len;
        for (double[] b : w.boxes) {
            if (Sim.ray(ax, ay, dx, dy, b) < len - 1e-6) return false;
        }
        return true;
    }

    public double wallAhead() {
        double rad = Math.toRadians(h), best = Specs.US_MAX_CM;
        for (double[] b : sim.world.boxes) {
            best = Math.min(best, Sim.ray(x, y, Math.cos(rad), Math.sin(rad), b));
        }
        return Math.max(0, best - RADIUS_CM);
    }

    static double angleTo(double deg) {
        return ((deg % 360) + 540) % 360 - 180;
    }
}
