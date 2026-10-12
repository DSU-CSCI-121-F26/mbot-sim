package mbot;

/**
 * A drone that flies a loop through a list of points, over and over, looking ahead as it goes.
 *
 * <pre>
 * new Patrol(40, 40,  260, 40,  260, 260,  40, 260)   // four corners: x, y, x, y, ...
 * </pre>
 */
public class Patrol extends Drone {

    private final double[] points;
    private int next = 1;

    /**
     * @param xy the points to visit, as pairs: x1, y1, x2, y2, ... (at least two points).
     *           The drone starts at the first one.
     */
    public Patrol(double... xy) {
        if (xy.length < 4 || xy.length % 2 != 0) {
            throw new IllegalArgumentException("A Patrol needs at least two points, given as x, y pairs: "
                    + "new Patrol(40, 40, 260, 40)");
        }
        points = xy.clone();
    }

    /** @return the first point, where the drone starts: {x, y} */
    public double[] start() {
        return new double[]{points[0], points[1]};
    }

    @Override
    public void tick() {
        double tx = points[2 * next], ty = points[2 * next + 1];
        double dx = tx - x(), dy = ty - y();
        if (Math.hypot(dx, dy) < 4) {
            next = (next + 1) % (points.length / 2);
            return;
        }
        // Steer toward the next point: turn harder the further off course it is
        double off = angleTo(Math.toDegrees(Math.atan2(dy, dx)) - heading());
        setTurnRate(off * 4);
        setSpeed(Math.abs(off) < 30 ? MAX_SPEED : 2);
    }

    /** An angle squeezed into -180..180. */
    static double angleTo(double deg) {
        double d = ((deg % 360) + 540) % 360 - 180;
        return d;
    }
}
