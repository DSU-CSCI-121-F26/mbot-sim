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

    public static final String[] BUILT_IN = {"open-room", "wall-ahead", "parking", "hallway", "obstacles", "line-track"};
    static final double BORDER_CM = 5;

    public final String name;
    public final Map<String, Object> raw;
    public final double width, height;
    public final double startX, startY, startHeading;
    public final List<double[]> boxes = new ArrayList<>();     // {x, y, w, h}, solid
    public final List<double[]> tape = new ArrayList<>();      // {x1, y1, x2, y2, width}
    public final List<double[]> cans = new ArrayList<>();      // {x, y, r}, solid round objects
    public final List<String> requires = new ArrayList<>();    // sensors this world is built for
    private final Map<String, List<double[]>> zones = new java.util.HashMap<>();

    @SuppressWarnings("unchecked")
    World(String name, Map<String, Object> raw) {
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
        for (Object o : (List<Object>) raw.getOrDefault("requires", List.of())) {
            requires.add(String.valueOf(o));
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

    /** Zones with this label, as {x, y, w, h}. */
    public List<double[]> zones(String label) {
        return zones.getOrDefault(label, List.of());
    }

    public String title() {
        return String.valueOf(raw.getOrDefault("title", name));
    }
}
