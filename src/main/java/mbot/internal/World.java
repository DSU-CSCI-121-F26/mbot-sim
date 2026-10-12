package mbot.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A room: its size, where the robot starts, solid boxes (walls), and tape on the floor.
 * All in cm. x grows to the right, y grows away from the viewer, heading 0 faces +x and
 * 90 faces +y. Positive turns are to the left, the same as the BRACE TURN command.
 *
 * World files are JSON. See docs/WORLDS.md for the format.
 */
public final class World {

    public static final String[] BUILT_IN = {"open-room", "wall-ahead", "parking", "hallway", "obstacles", "line-track",
            "color-zones", "junctions"};
    public static final java.util.Set<String> COLORS = java.util.Set.of("red", "green", "blue", "yellow");

    /** A drone placed on the board, and where it starts. */
    public record DroneSpec(mbot.Drone drone, double x, double y, double heading) {}

    /** A colored patch of floor, for the ColorSensor. */
    public record Patch(double x, double y, double w, double h, String color) {}
    static final double BORDER_CM = 5;

    public final String name;
    public final Map<String, Object> raw;
    public final double width, height;
    public final double startX, startY, startHeading;
    public final List<double[]> boxes = new ArrayList<>();     // {x, y, w, h}, solid
    public final List<double[]> tape = new ArrayList<>();      // {x1, y1, x2, y2, width}
    public final List<double[]> cans = new ArrayList<>();      // {x, y, r}, solid round objects
    public final List<String> requires = new ArrayList<>();    // sensors this world is built for
    public final List<Patch> patches = new ArrayList<>();      // colored floor, later ones on top
    public final List<DroneSpec> drones = new ArrayList<>();
    public final int maxTags;                                  // tags before the robot shuts down
    public final int maxBumps;                                 // -1 = no limit
    public final double timeLimit;                             // robot seconds; infinity = no limit
    private final Map<String, List<double[]>> zones = new java.util.HashMap<>();

    World(String name, Map<String, Object> raw) {
        this(name, raw, List.of());
    }

    /** A world built from a Board: its JSON-shaped description plus the drone objects it placed. */
    @SuppressWarnings("unchecked")
    public World(String name, Map<String, Object> raw, List<DroneSpec> placed) {
        this.name = name;
        this.raw = raw;
        List<Object> size = (List<Object>) raw.get("size");
        width = d(size.get(0));
        height = d(size.get(1));
        Map<String, Object> start = (Map<String, Object>) raw.get("start");
        startX = d(start.get("x"));
        startY = d(start.get("y"));
        startHeading = d(start.getOrDefault("heading", 90.0));

        // The room's own walls, just outside the floor.
        boxes.add(new double[]{-BORDER_CM, -BORDER_CM, width + 2 * BORDER_CM, BORDER_CM});
        boxes.add(new double[]{-BORDER_CM, height, width + 2 * BORDER_CM, BORDER_CM});
        boxes.add(new double[]{-BORDER_CM, 0, BORDER_CM, height});
        boxes.add(new double[]{width, 0, BORDER_CM, height});

        for (Object o : (List<Object>) raw.getOrDefault("walls", List.of())) {
            Map<String, Object> w = (Map<String, Object>) o;
            boxes.add(new double[]{d(w.get("x")), d(w.get("y")), d(w.get("w")), d(w.get("h"))});
        }
        // Objects to detect: boxes are solid like walls; cans are round.
        for (Object o : (List<Object>) raw.getOrDefault("objects", List.of())) {
            Map<String, Object> ob = (Map<String, Object>) o;
            if ("can".equals(ob.get("shape"))) {
                cans.add(new double[]{d(ob.get("x")), d(ob.get("y")), d(ob.getOrDefault("r", 4.0))});
            } else {
                boxes.add(new double[]{d(ob.get("x")), d(ob.get("y")), d(ob.get("w")), d(ob.get("h"))});
            }
        }
        for (Object o : (List<Object>) raw.getOrDefault("zones", List.of())) {
            Map<String, Object> z = (Map<String, Object>) o;
            zones.computeIfAbsent(String.valueOf(z.get("label")), k -> new ArrayList<>())
                    .add(new double[]{d(z.get("x")), d(z.get("y")), d(z.get("w")), d(z.get("h"))});
        }
        for (Object o : (List<Object>) raw.getOrDefault("patches", List.of())) {
            Map<String, Object> pt = (Map<String, Object>) o;
            String color = String.valueOf(pt.get("color"));
            if (!COLORS.contains(color)) {
                throw new IllegalArgumentException("World " + name + ": patch color must be one of " + COLORS + ", not " + color);
            }
            patches.add(new Patch(d(pt.get("x")), d(pt.get("y")), d(pt.get("w")), d(pt.get("h")), color));
        }
        for (Object o : (List<Object>) raw.getOrDefault("requires", List.of())) {
            requires.add(String.valueOf(o));
        }
        Map<String, Object> rules = (Map<String, Object>) raw.getOrDefault("rules", Map.of());
        maxTags = (int) d(rules.getOrDefault("maxTags", 3.0));
        maxBumps = (int) d(rules.getOrDefault("maxBumps", -1.0));
        timeLimit = rules.containsKey("timeLimit") ? d(rules.get("timeLimit")) : Double.POSITIVE_INFINITY;
        drones.addAll(placed);
        if (placed.isEmpty()) {
            for (Object o : (List<Object>) raw.getOrDefault("drones", List.of())) {
                drones.add(droneFromJson((Map<String, Object>) o));
            }
        }
        for (Object o : (List<Object>) raw.getOrDefault("tape", List.of())) {
            Map<String, Object> t = (Map<String, Object>) o;
            double tw = d(t.getOrDefault("width", 2.0));
            List<Object> pts = (List<Object>) t.get("points");
            boolean closed = Boolean.TRUE.equals(t.get("closed"));
            int n = pts.size();
            for (int i = 0; i < (closed ? n : n - 1); i++) {
                List<Object> a = (List<Object>) pts.get(i), b = (List<Object>) pts.get((i + 1) % n);
                tape.add(new double[]{d(a.get(0)), d(a.get(1)), d(b.get(0)), d(b.get(1)), tw});
            }
        }
    }

    static double d(Object o) {
        return ((Number) o).doubleValue();
    }

    /**
     * A built-in world by name, or a JSON file path (relative to the project folder).
     */
    @SuppressWarnings("unchecked")
    public static World load(String name) {
        String text;
        try {
            if (name.endsWith(".json")) {
                text = Files.readString(Path.of(name), StandardCharsets.UTF_8);
            } else {
                try (InputStream in = World.class.getResourceAsStream("/mbot/worlds/" + name + ".json")) {
                    if (in == null) {
                        throw new IllegalArgumentException("There is no world called \"" + name
                                + "\". Try one of: " + String.join(", ", BUILT_IN));
                    }
                    text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the world file " + name + ": " + e.getMessage());
        }
        return new World(name, (Map<String, Object>) Json.parse(text));
    }

    /** The built-in drones, from a JSON world file. */
    @SuppressWarnings("unchecked")
    static DroneSpec droneFromJson(Map<String, Object> o) {
        String type = String.valueOf(o.getOrDefault("type", "sentry"));
        double heading = d(o.getOrDefault("heading", 90.0));
        mbot.Drone drone = switch (type) {
            case "sentry" -> new mbot.Sentry(d(o.getOrDefault("spin", 45.0)));
            case "hunter" -> new mbot.Hunter();
            case "patrol" -> {
                List<Object> pts = (List<Object>) o.get("points");
                double[] xy = new double[pts.size()];
                for (int i = 0; i < xy.length; i++) xy[i] = d(pts.get(i));
                yield new mbot.Patrol(xy);
            }
            default -> throw new IllegalArgumentException("Unknown drone type \"" + type
                    + "\". Use sentry, patrol or hunter.");
        };
        if (drone instanceof mbot.Patrol p) {
            double[] st = p.start();
            return new DroneSpec(drone, st[0], st[1], heading);
        }
        return new DroneSpec(drone, d(o.get("x")), d(o.get("y")), heading);
    }

    /** A student's board: make one with its no-argument constructor and build the world from it. */
    public static World fromBoard(Class<? extends mbot.Board> type) {
        try {
            java.lang.reflect.Constructor<? extends mbot.Board> c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance().toWorld();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getSimpleName() + " needs a constructor with no parameters: public "
                    + type.getSimpleName() + "() { super(\"name\", \"you\"); ... }");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException r) throw r;
            throw new IllegalStateException("Building " + type.getSimpleName() + " failed: " + cause, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not build " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /** Zones with this label, as {x, y, w, h}. */
    public List<double[]> zones(String label) {
        return zones.getOrDefault(label, List.of());
    }

    public String title() {
        return String.valueOf(raw.getOrDefault("title", name));
    }
}
