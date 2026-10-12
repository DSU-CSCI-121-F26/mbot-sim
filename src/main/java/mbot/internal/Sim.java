package mbot.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * The robot's body in a world: where it is, how its wheels are turning, and what its
 * sensors see. No recording and no threads here, so it is easy to test.
 *
 * Every motion method returns the poses it passed through, as {x, y, heading} triples,
 * so the viewer can replay the exact path.
 */
public final class Sim {

    static final double DT_S = 0.01;
    static final double SAMPLE_S = 0.05;
    static final double DRONE_TICK_S = 0.1;

    public final World world;
    public double x, y, h;              // cm, cm, degrees (0 = +x, left turns positive)
    public double left, right;          // wheel speeds in cm/s while driving freely
    public double time;                 // robot seconds since the program started
    public boolean bumped;              // set by the last motion if it hit something

    public final List<DroneBody> drones = new ArrayList<>();
    public int tags;
    public int maxTags;
    public double timeLimit;
    public String shutdown;             // null while running; "tags" or "time" once it has shut down
    final List<double[]> tagEvents = new ArrayList<>();   // {time, drone index}, drained per frame
    private double droneClock;

    public Sim(World world) {
        this.world = world;
        x = world.startX;
        y = world.startY;
        h = world.startHeading;
        maxTags = world.maxTags;
        timeLimit = world.timeLimit;
        java.util.Map<String, Integer> seen = new java.util.HashMap<>();
        for (World.DroneSpec d : world.drones) {
            seen.merge(d.drone().getName(), 1, Integer::sum);
        }
        java.util.Map<String, Integer> count = new java.util.HashMap<>();
        for (World.DroneSpec d : world.drones) {
            String base = d.drone().getName();
            String name = seen.get(base) > 1 ? base + " " + count.merge(base, 1, Integer::sum) : base;
            drones.add(new DroneBody(d.drone(), name, d.x(), d.y(), d.heading(), this));
        }
        for (DroneBody d : drones) d.look();
    }

    public boolean moving() {
        return left != 0 || right != 0;
    }

    double[] pose() {
        return new double[]{x, y, h};
    }

    // ------------------------------------------------------------------ the rest of the world

    /**
     * Everything that is not the robot moves on: drones fly and look, tags land, the clock runs.
     * Called once per small time step from every motion, and from idle().
     */
    void stepWorld(double dt) {
        time += dt;
        if (drones.isEmpty()) {
            if (shutdown == null && time > timeLimit + 1e-9) shutdown = "time";
            return;
        }
        droneClock += dt;
        while (droneClock >= DRONE_TICK_S - 1e-9) {
            droneClock -= DRONE_TICK_S;
            for (DroneBody d : drones) {
                d.look();
                d.drone.tick();
                d.samples.add(new double[]{time, d.x, d.y, d.h, d.seeing ? 1 : 0});
            }
        }
        for (int i = 0; i < drones.size(); i++) {
            DroneBody d = drones.get(i);
            d.move(dt);
            d.look();
            d.cooldown -= dt;
            d.watch = d.seeing ? d.watch + dt : 0;
            if (shutdown == null && d.watch >= DroneBody.TAG_AFTER_S && d.cooldown <= 0) {
                tags++;
                tagEvents.add(new double[]{time, i});
                d.cooldown = DroneBody.COOLDOWN_S;
                d.watch = 0;
                if (tags >= maxTags) shutdown = "tags";
            }
        }
        if (shutdown == null && time > timeLimit + 1e-9) shutdown = "time";
    }

    /** The robot sits still while time passes (a sensor read, a scan, a Thread.sleep). */
    public void idle(double seconds) {
        double t = 0;
        while (t < seconds - 1e-9 && shutdown == null) {
            double dt = Math.min(DT_S, seconds - t);
            stepWorld(dt);
            t += dt;
        }
    }

    // ------------------------------------------------------------------ motion

    /** Drive a set distance (negative = backward) at the straight() speed. */
    public List<double[]> straight(double cm) {
        bumped = false;
        List<double[]> path = new ArrayList<>();
        path.add(pose());
        double dir = Math.signum(cm), rad = Math.toRadians(h), v = Specs.STRAIGHT_CM_PER_S;
        double done = 0, total = Math.abs(cm);
        while (done < total - 1e-9 && shutdown == null) {
            double step = Math.min(v * DT_S, total - done);
            double nx = x + dir * step * Math.cos(rad), ny = y + dir * step * Math.sin(rad);
            if (hits(nx, ny)) {
                bumped = true;
                break;
            }
            x = nx;
            y = ny;
            done += step;
            stepWorld(step / v);
        }
        path.add(pose());
        return path;
    }

    /** Spin on the spot. Positive degrees turn left. A round body never hits anything turning. */
    public List<double[]> turn(double degrees) {
        bumped = false;
        List<double[]> path = new ArrayList<>();
        path.add(pose());
        double start = h, turned = 0, total = Math.abs(degrees), dir = Math.signum(degrees);
        while (turned < total - 1e-9 && shutdown == null) {
            double step = Math.min(Specs.TURN_DEG_PER_S * DT_S, total - turned);
            turned += step;
            h = norm(start + dir * turned);
            stepWorld(step / Specs.TURN_DEG_PER_S);
        }
        double[] end = pose();
        end[2] = start + dir * turned;      // keep the unwrapped angle so the viewer spins the right way
        path.add(end);
        return path;
    }

    /** Let the wheels run at their current speeds for this many seconds. */
    public List<double[]> run(double seconds) {
        bumped = false;
        List<double[]> path = new ArrayList<>();
        path.add(pose());
        double t = 0, sinceSample = 0, unwrapped = h;
        while (t < seconds - 1e-9 && shutdown == null) {
            double dt = Math.min(DT_S, seconds - t);
            double v = (left + right) / 2, w = Math.toDegrees((right - left) / Specs.TRACK_CM);
            double rad = Math.toRadians(h);
            double nx = x + v * dt * Math.cos(rad), ny = y + v * dt * Math.sin(rad);
            if (v != 0 && hits(nx, ny)) {
                bumped = true;
                left = right = 0;
                stepWorld(dt);
                break;
            }
            x = nx;
            y = ny;
            h = norm(h + w * dt);
            unwrapped += w * dt;
            t += dt;
            stepWorld(dt);
            sinceSample += dt;
            if (sinceSample >= SAMPLE_S - 1e-9) {
                path.add(new double[]{x, y, unwrapped});
                sinceSample = 0;
            }
        }
        if (shutdown != null) left = right = 0;
        double[] last = path.get(path.size() - 1);
        if (last[0] != x || last[1] != y || last[2] != unwrapped) path.add(new double[]{x, y, unwrapped});
        return path;
    }

    // ------------------------------------------------------------------ the mission

    /** Robot centre inside a zone with this label? */
    public boolean inZone(String label) {
        for (double[] z : world.zones(label)) {
            if (x >= z[0] && x <= z[0] + z[2] && y >= z[1] && y <= z[1] + z[3]) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ sensors

    /** Ultrasonic: nearest solid thing inside the sensor's cone. Returns {distance, hitX, hitY}. */
    public double[] ultrasonic() {
        double rad = Math.toRadians(h);
        double sx = x + Specs.US_OFFSET_CM * Math.cos(rad), sy = y + Specs.US_OFFSET_CM * Math.sin(rad);
        double best = Specs.US_MAX_CM, hx = Double.NaN, hy = Double.NaN;
        for (double a = -Specs.US_HALF_ANGLE_DEG; a <= Specs.US_HALF_ANGLE_DEG + 1e-9; a += 1) {
            double r = Math.toRadians(h + a), dx = Math.cos(r), dy = Math.sin(r);
            for (double[] b : world.boxes) {
                double t = ray(sx, sy, dx, dy, b);
                if (t < best) {
                    best = t;
                    hx = sx + dx * t;
                    hy = sy + dy * t;
                }
            }
            for (double[] c : world.cans) {
                double t = rayCircle(sx, sy, dx, dy, c);
                if (t < best) {
                    best = t;
                    hx = sx + dx * t;
                    hy = sy + dy * t;
                }
            }
        }
        return new double[]{Math.max(Specs.US_MIN_CM, best), hx, hy};
    }

    /**
     * An ultrasonic reading in any direction: relAngle degrees from the robot's heading
     * (left positive), from a sensor mounted offsetCm ahead of the centre, with the given cone.
     * Returns {distance, hitX, hitY}.
     */
    public double[] ultrasonicAt(double relAngle, double offsetCm, double halfAngle) {
        double rad = Math.toRadians(h);
        double sx = x + offsetCm * Math.cos(rad), sy = y + offsetCm * Math.sin(rad);
        double best = Specs.US_MAX_CM, hx = Double.NaN, hy = Double.NaN;
        for (double a = -halfAngle; a <= halfAngle + 1e-9; a += 1) {
            double r = Math.toRadians(h + relAngle + a), dx = Math.cos(r), dy = Math.sin(r);
            for (double[] b : world.boxes) {
                double t = ray(sx, sy, dx, dy, b);
                if (t < best) { best = t; hx = sx + dx * t; hy = sy + dy * t; }
            }
            for (double[] c : world.cans) {
                double t = rayCircle(sx, sy, dx, dy, c);
                if (t < best) { best = t; hx = sx + dx * t; hy = sy + dy * t; }
            }
        }
        return new double[]{Math.max(Specs.US_MIN_CM, best), hx, hy};
    }

    /** Floor color under the front-centre of the robot: tape is black, then the topmost patch, else white. */
    public String colorUnder() {
        double rad = Math.toRadians(h);
        double px = x + Specs.LINE_AHEAD_CM * Math.cos(rad), py = y + Specs.LINE_AHEAD_CM * Math.sin(rad);
        if (onTape(px, py)) return "black";
        for (int i = world.patches.size() - 1; i >= 0; i--) {
            World.Patch p = world.patches.get(i);
            if (px >= p.x() && px <= p.x() + p.w() && py >= p.y() && py <= p.y() + p.h()) return p.color();
        }
        return "white";
    }

    /** Line sensor: bit 3 = far left probe ... bit 0 = far right probe. 1 = over tape. */
    public int lineStatus() {
        int bits = 0;
        for (double[] p : probes()) {
            bits = (bits << 1) | (onTape(p[0], p[1]) ? 1 : 0);
        }
        return bits;
    }

    /** Where the four line probes are on the floor, far left first. */
    public double[][] probes() {
        double rad = Math.toRadians(h), c = Math.cos(rad), s = Math.sin(rad);
        double[][] out = new double[Specs.LINE_PROBES_CM.length][];
        for (int i = 0; i < out.length; i++) {
            double side = Specs.LINE_PROBES_CM[i];
            out[i] = new double[]{
                    x + Specs.LINE_AHEAD_CM * c - side * s,
                    y + Specs.LINE_AHEAD_CM * s + side * c};
        }
        return out;
    }

    // ------------------------------------------------------------------ geometry

    boolean hits(double cx, double cy) {
        for (double[] b : world.boxes) {
            double nx = clamp(cx, b[0], b[0] + b[2]), ny = clamp(cy, b[1], b[1] + b[3]);
            double dx = cx - nx, dy = cy - ny;
            if (dx * dx + dy * dy < Specs.RADIUS_CM * Specs.RADIUS_CM) return true;
        }
        for (double[] c : world.cans) {
            double r = Specs.RADIUS_CM + c[2], dx = cx - c[0], dy = cy - c[1];
            if (dx * dx + dy * dy < r * r) return true;
        }
        return false;
    }

    /** Distance along the ray (dx, dy unit) to the circle {x, y, r}, or infinity. */
    static double rayCircle(double ox, double oy, double dx, double dy, double[] c) {
        double fx = ox - c[0], fy = oy - c[1];
        double b = fx * dx + fy * dy, cc = fx * fx + fy * fy - c[2] * c[2];
        double disc = b * b - cc;
        if (disc < 0) return Double.POSITIVE_INFINITY;
        double t = -b - Math.sqrt(disc);
        if (t < 0) t = -b + Math.sqrt(disc);
        return t < 0 ? Double.POSITIVE_INFINITY : t;
    }

    boolean onTape(double px, double py) {
        for (double[] t : world.tape) {
            if (segDist(px, py, t[0], t[1], t[2], t[3]) <= t[4] / 2) return true;
        }
        return false;
    }

    /** Distance along the ray (dx, dy unit) to the box, or infinity. Slab method. */
    static double ray(double ox, double oy, double dx, double dy, double[] b) {
        double tmin = Double.NEGATIVE_INFINITY, tmax = Double.POSITIVE_INFINITY;
        double[] o = {ox, oy}, d = {dx, dy}, lo = {b[0], b[1]}, hi = {b[0] + b[2], b[1] + b[3]};
        for (int k = 0; k < 2; k++) {
            if (Math.abs(d[k]) < 1e-12) {
                if (o[k] < lo[k] || o[k] > hi[k]) return Double.POSITIVE_INFINITY;
            } else {
                double t1 = (lo[k] - o[k]) / d[k], t2 = (hi[k] - o[k]) / d[k];
                tmin = Math.max(tmin, Math.min(t1, t2));
                tmax = Math.min(tmax, Math.max(t1, t2));
            }
        }
        if (tmax < Math.max(tmin, 0)) return Double.POSITIVE_INFINITY;
        return Math.max(tmin, 0);
    }

    static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double vx = bx - ax, vy = by - ay, len2 = vx * vx + vy * vy;
        double t = len2 == 0 ? 0 : clamp(((px - ax) * vx + (py - ay) * vy) / len2, 0, 1);
        double dx = px - (ax + t * vx), dy = py - (ay + t * vy);
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double norm(double deg) {
        double d = deg % 360;
        return d < 0 ? d + 360 : d;
    }
}
