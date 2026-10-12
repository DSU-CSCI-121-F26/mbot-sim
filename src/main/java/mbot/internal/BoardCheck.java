package mbot.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Catches boards that cannot be played before the robot moves: a start inside a wall, a goal
 * the robot can never reach, a sensor name nobody has. Problems are reported, not thrown, so a
 * student can still drive on a half-finished board.
 *
 * Reachability is a breadth-first search over a 2.5 cm grid of places the robot's centre can be
 * without touching anything. It ignores how the robot turns, so "reachable" means "there is
 * room", not "this route is easy".
 */
public final class BoardCheck {

    static final double GRID_CM = 2.5;     // fine enough to find the middle of a 20 cm corridor
    public static final Set<String> PARTS = Set.of("ultrasonic", "line", "scanner", "color", "radar");

    private BoardCheck() {}

    public static List<String> problems(World w) {
        List<String> out = new ArrayList<>();
        if (fits(w, w.startX, w.startY) == false) {
            out.add("the start (" + Session.num(w.startX) + ", " + Session.num(w.startY)
                    + ") is inside or touching something solid. The robot needs 9 cm of room all round.");
        }
        for (String p : w.requires) {
            if (!PARTS.contains(p)) out.add("requires(\"" + p + "\") is not a sensor. Use " + PARTS + ".");
        }
        List<double[]> goals = w.zones("Goal");
        if (!goals.isEmpty() && out.isEmpty() && !reachable(w, goals)) {
            out.add("the Goal cannot be reached from the start: no path is wide enough for the robot (17 cm). "
                    + "Make the corridors at least 1 cell wider, or move the walls.");
        }
        return out;
    }

    static boolean reachable(World w, List<double[]> goals) {
        int cols = (int) Math.ceil(w.width / GRID_CM), rows = (int) Math.ceil(w.height / GRID_CM);
        boolean[][] seen = new boolean[cols][rows];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int sc = clampIndex((int) (w.startX / GRID_CM), cols), sr = clampIndex((int) (w.startY / GRID_CM), rows);
        queue.add(new int[]{sc, sr});
        seen[sc][sr] = true;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            double cx = (c[0] + 0.5) * GRID_CM, cy = (c[1] + 0.5) * GRID_CM;
            for (double[] g : goals) {
                if (cx >= g[0] && cx <= g[0] + g[2] && cy >= g[1] && cy <= g[1] + g[3]) return true;
            }
            for (int[] s : steps) {
                int nc = c[0] + s[0], nr = c[1] + s[1];
                if (nc < 0 || nr < 0 || nc >= cols || nr >= rows || seen[nc][nr]) continue;
                seen[nc][nr] = true;
                if (fits(w, (nc + 0.5) * GRID_CM, (nr + 0.5) * GRID_CM)) queue.add(new int[]{nc, nr});
            }
        }
        return false;
    }

    /** The robot's round body at (cx, cy) touches nothing solid. Same rule as Sim.hits. */
    static boolean fits(World w, double cx, double cy) {
        double r = Specs.RADIUS_CM;
        for (double[] b : w.boxes) {
            double nx = Sim.clamp(cx, b[0], b[0] + b[2]), ny = Sim.clamp(cy, b[1], b[1] + b[3]);
            double dx = cx - nx, dy = cy - ny;
            if (dx * dx + dy * dy < r * r) return false;
        }
        for (double[] c : w.cans) {
            double rr = r + c[2], dx = cx - c[0], dy = cy - c[1];
            if (dx * dx + dy * dy < rr * rr) return false;
        }
        return true;
    }

    private static int clampIndex(int i, int n) {
        return Math.max(0, Math.min(n - 1, i));
    }
}
