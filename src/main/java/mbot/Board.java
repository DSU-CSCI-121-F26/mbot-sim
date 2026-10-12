package mbot;

import mbot.internal.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build your own board: walls, obstacles, tape, colored floor, a goal, and drones.
 * Write a class that extends Board and set everything up in its constructor:
 *
 * <pre>
 * public class SpiralMaze extends Board {
 *     public SpiralMaze() {
 *         super("Spiral maze", "Tariq");     // the board's name, and who made it
 *         map(
 *             "##########",
 *             "#S...#..G#",
 *             "#.##.#.###",
 *             "#.#..o...#",
 *             "#...##...#",
 *             "##########"
 *         );
 *         sentry(140, 60);
 *         maxTags(3);
 *     }
 * }
 * </pre>
 *
 * Then drive on it with {@code @MBotLab(board = SpiralMaze.class)}.
 *
 * <h2>Map characters</h2>
 * Each character is one square, {@link #cellSize(double)} cm wide (20 cm unless you change it).
 * The top row of the map is the far side of the board.
 * <pre>
 *   #  wall                 .  floor (a space works too)
 *   S  start, facing up     &lt; &gt; ^ v   start, facing that way
 *   G  goal (green floor)   o  a can (round, solid)     x  a box (solid)
 *   -  tape, left to right  |  tape, up and down        +  tape both ways
 *   r g b y  red, green, blue or yellow floor
 *   D  a Sentry drone
 * </pre>
 *
 * <h2>Measurements</h2>
 * Everything is in cm. x grows to the right and y grows up the board (away from you).
 * (0, 0) is the near-left corner. Headings: 0 faces right, 90 faces up.
 */
public abstract class Board {

    private final String name;
    private final String author;
    private double cell = 20;
    private double width = -1, height = -1;
    private double startX = -1, startY = -1, startHeading = 90;
    private final List<Object> walls = new ArrayList<>();
    private final List<Object> objects = new ArrayList<>();
    private final List<Object> tape = new ArrayList<>();
    private final List<Object> patches = new ArrayList<>();
    private final List<Object> zones = new ArrayList<>();
    private final List<World.DroneSpec> drones = new ArrayList<>();
    private final List<String> requires = new ArrayList<>();
    private final Map<String, Object> rules = new LinkedHashMap<>();
    private String about = "";

    /**
     * @param name the board's name, shown in the viewer
     * @param author who made it
     */
    protected Board(String name, String author) {
        this.name = name;
        this.author = author;
    }

    // ------------------------------------------------------------------ drawing with a map

    /** How big one map character is, in cm. Call this before {@link #map}. Default 20. */
    protected final void cellSize(double cm) {
        if (cm < 10) throw new IllegalArgumentException("cellSize must be at least 10 cm, so the robot (17 cm) fits in a 2-cell corridor");
        cell = cm;
    }

    /**
     * Draw the board with text, one string per row, top row = far side.
     * Sets the board's size from the map. See the class description for the characters.
     */
    protected final void map(String... rows) {
        if (rows.length == 0) throw new IllegalArgumentException("map() needs at least one row");
        int cols = 0;
        for (String r : rows) cols = Math.max(cols, r.length());
        width = cols * cell;
        height = rows.length * cell;
        List<int[]> goalCells = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            double y0 = (rows.length - 1 - r) * cell;           // top row is the far side
            int c = 0;
            while (c < row.length()) {
                char ch = row.charAt(c);
                if (ch == '#') {                                 // merge runs of wall into one box
                    int end = c;
                    while (end < row.length() && row.charAt(end) == '#') end++;
                    wall(c * cell, y0, (end - c) * cell, cell);
                    c = end;
                    continue;
                }
                double x0 = c * cell, cx = x0 + cell / 2, cy = y0 + cell / 2;
                switch (ch) {
                    case '.', ' ' -> { }
                    case 'S', '^' -> start(cx, cy, 90);
                    case '>' -> start(cx, cy, 0);
                    case '<' -> start(cx, cy, 180);
                    case 'v', 'V' -> start(cx, cy, 270);
                    case 'G' -> goalCells.add(new int[]{c, rows.length - 1 - r});   // merged below
                    case 'o' -> can(cx, cy);
                    case 'x' -> box(x0 + cell * 0.15, y0 + cell * 0.15, cell * 0.7, cell * 0.7);
                    case '-' -> tape(x0, cy, x0 + cell, cy);
                    case '|' -> tape(cx, y0, cx, y0 + cell);
                    case '+' -> {
                        tape(x0, cy, x0 + cell, cy);
                        tape(cx, y0, cx, y0 + cell);
                    }
                    case 'r' -> floor("red", x0, y0, cell, cell);
                    case 'g' -> floor("green", x0, y0, cell, cell);
                    case 'b' -> floor("blue", x0, y0, cell, cell);
                    case 'y' -> floor("yellow", x0, y0, cell, cell);
                    case 'D' -> sentry(cx, cy);
                    default -> throw new IllegalArgumentException("Map row " + (r + 1) + " has '" + ch
                            + "', which is not a map character. Use # . S G o x - | + r g b y D or < > ^ v");
                }
                c++;
            }
        }
        mergeGoals(goalCells);
    }

    /** Touching G squares become one goal (their bounding box), so a 3x2 block of G is one zone. */
    private void mergeGoals(List<int[]> cells) {
        boolean[] used = new boolean[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            if (used[i]) continue;
            int minC = cells.get(i)[0], maxC = minC, minR = cells.get(i)[1], maxR = minR;
            java.util.ArrayDeque<Integer> todo = new java.util.ArrayDeque<>(List.of(i));
            used[i] = true;
            while (!todo.isEmpty()) {
                int[] a = cells.get(todo.poll());
                minC = Math.min(minC, a[0]); maxC = Math.max(maxC, a[0]);
                minR = Math.min(minR, a[1]); maxR = Math.max(maxR, a[1]);
                for (int j = 0; j < cells.size(); j++) {
                    int[] b = cells.get(j);
                    if (!used[j] && Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1) {
                        used[j] = true;
                        todo.add(j);
                    }
                }
            }
            goal(minC * cell, minR * cell, (maxC - minC + 1) * cell, (maxR - minR + 1) * cell);
        }
    }

    // ------------------------------------------------------------------ placing things exactly

    /** The size of the floor, in cm. Not needed if you use {@link #map}. */
    protected final void size(double widthCm, double heightCm) {
        width = widthCm;
        height = heightCm;
    }

    /** Where the robot starts, and which way it faces (90 = up the board). */
    protected final void start(double x, double y, double heading) {
        startX = x;
        startY = y;
        startHeading = heading;
    }

    /** A wall: a solid box. (x, y) is its near-left corner. */
    protected final void wall(double x, double y, double w, double h) {
        walls.add(rect(x, y, w, h));
    }

    /** A box to detect: solid, lower than a wall. (x, y) is its near-left corner. */
    protected final void box(double x, double y, double w, double h) {
        Map<String, Object> b = rect(x, y, w, h);
        b.put("shape", "box");
        b.put("color", "#c8823a");
        objects.add(b);
    }

    /** A can: round and solid, 4 cm radius, centred at (x, y). */
    protected final void can(double x, double y) {
        can(x, y, 4);
    }

    /** A can with its own radius. */
    protected final void can(double x, double y, double radius) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("shape", "can");
        c.put("x", x);
        c.put("y", y);
        c.put("r", radius);
        c.put("color", objects.size() % 2 == 0 ? "#d6453d" : "#3d7dd6");
        objects.add(c);
    }

    /** A straight strip of tape from (x1, y1) to (x2, y2), for the line sensor. */
    protected final void tape(double x1, double y1, double x2, double y2) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("points", List.of(List.of(x1, y1), List.of(x2, y2)));
        t.put("width", 3.0);
        tape.add(t);
    }

    /** A patch of colored floor, for the color sensor: "red", "green", "blue" or "yellow". */
    protected final void floor(String color, double x, double y, double w, double h) {
        Map<String, Object> p = rect(x, y, w, h);
        p.put("color", color);
        patches.add(p);
    }

    /**
     * The goal: the robot passes the board if it ends with its centre inside.
     * The goal's floor is green, so a robot with a ColorSensor can tell when it has arrived.
     */
    protected final void goal(double x, double y, double w, double h) {
        zone("Goal", x, y, w, h);
        floor("green", x, y, w, h);
    }

    /** A labelled area. Tests can ask {@code Testing.insideZone(label)}. */
    protected final void zone(String label, double x, double y, double w, double h) {
        Map<String, Object> z = rect(x, y, w, h);
        z.put("label", label);
        zones.add(z);
    }

    // ------------------------------------------------------------------ drones

    /** A Sentry drone at (x, y): it stays put and spins. */
    protected final void sentry(double x, double y) {
        add(new Sentry(), x, y, 90);
    }

    /** A Patrol drone that loops through points given as x, y pairs. */
    protected final void patrol(double... xy) {
        Patrol p = new Patrol(xy);
        double[] s = p.start();
        double heading = Math.toDegrees(Math.atan2(xy[3] - xy[1], xy[2] - xy[0]));
        add(p, s[0], s[1], heading);
    }

    /** A Hunter drone at (x, y): it searches, chases what it sees, and loses track behind walls. */
    protected final void hunter(double x, double y) {
        add(new Hunter(), x, y, 90);
    }

    /** Any drone, including your own class that extends Drone. */
    protected final void add(Drone drone, double x, double y, double heading) {
        if (drone == null) throw new IllegalArgumentException("add() was given a null drone. Make it with new first.");
        drones.add(new World.DroneSpec(drone, x, y, heading));
    }

    // ------------------------------------------------------------------ rules

    /** Tags before the robot shuts down. Default 3. */
    protected final void maxTags(int tags) {
        rules.put("maxTags", (double) tags);
    }

    /** The most bumps allowed to pass the board. Default: no limit. */
    protected final void maxBumps(int bumps) {
        rules.put("maxBumps", (double) bumps);
    }

    /** The robot shuts down after this many seconds of robot time. Default: no limit. */
    protected final void timeLimit(double seconds) {
        rules.put("timeLimit", seconds);
    }

    /** Sensors a robot needs for this board: "ultrasonic", "line", "scanner", "color", "radar". */
    protected final void requires(String... parts) {
        requires.addAll(List.of(parts));
    }

    /** One sentence about the board, shown in the viewer. */
    protected final void about(String sentence) {
        about = sentence;
    }

    // ------------------------------------------------------------------ for the simulator

    /** Turn this board into a world. Not for students: the simulator calls it. */
    public final World toWorld() {
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException(getClass().getSimpleName() + " has no size. Call map(...) or size(width, height) in its constructor.");
        }
        if (startX < 0) {
            throw new IllegalStateException(getClass().getSimpleName() + " has no start. Put an S in the map, or call start(x, y, heading).");
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("title", name);
        raw.put("author", author);
        if (!about.isEmpty()) raw.put("about", about);
        raw.put("size", List.of(width, height));
        Map<String, Object> st = new LinkedHashMap<>();
        st.put("x", startX);
        st.put("y", startY);
        st.put("heading", startHeading);
        raw.put("start", st);
        raw.put("walls", walls);
        raw.put("objects", objects);
        raw.put("tape", tape);
        raw.put("patches", patches);
        raw.put("zones", zones);
        raw.put("requires", requires);
        raw.put("rules", rules);
        return new World(getClass().getSimpleName(), raw, drones);
    }

    private static Map<String, Object> rect(double x, double y, double w, double h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("w", w);
        m.put("h", h);
        return m;
    }
}
