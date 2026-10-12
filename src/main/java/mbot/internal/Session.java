package mbot.internal;

import mbot.MBotLab;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One run of a student's program: the "auto-configuration" behind {@code new MBot2()}.
 *
 * The first MBot2 reads @MBotLab off the main class, loads the world, starts recording,
 * and registers a shutdown hook. Each call on MBot2 lands here, is simulated, and becomes
 * one frame tagged with the student's file and line. When main() returns, the hook saves
 * the run and makes sure the viewer is up. The student writes none of this.
 */
public final class Session {

    private static Session current;

    final World world;
    final Sim sim;
    final Sources sources = new Sources();
    final List<Map<String, Object>> frames = new ArrayList<>();
    final Set<String> parts = new LinkedHashSet<>();    // sensors plugged in: "ultrasonic", "line"
    private int robots, bumps, reads;
    private Object robot;                                // the first robot made: its class is the student's design
    private final String mainName;
    private final boolean live;             // false in tests: no hooks, no files, no viewer
    private final PrintStream console;      // the real System.out, for the library's own messages
    private Tee tee;
    private long lastCallNanos = System.nanoTime();
    private volatile boolean finished;

    Session(World world, String mainName, boolean live) {
        this.world = world;
        this.sim = new Sim(world);
        this.mainName = mainName;
        this.live = live;
        this.console = System.out;
    }

    /** A session with no hooks, files or viewer: for tests and the instructor's tools. */
    public static Session headless(String worldName) {
        return new Session(World.load(worldName), "Headless", false);
    }

    /** End a headless session (a live one ends itself when main returns). */
    public void end() {
        finish();
    }

    /**
     * Called by every {@code new Chassis()} / {@code new MBot2()}. The first one starts the session
     * (or joins the one {@link #startTesting} made); later ones share it, since there is one robot.
     */
    public static synchronized Session start(Object robot) {
        if (current == null) {
            Class<?> main = Sources.mainClass();
            MBotLab lab = main == null ? null : main.getAnnotation(MBotLab.class);
            String worldName = lab == null ? "open-room" : lab.world();
            Session s = new Session(World.load(worldName), main == null ? "Main" : main.getSimpleName(), true);
            current = s;
            if (lab == null) {
                s.console.println("[mbot] No @MBotLab on " + s.mainName + ", so using the \"open-room\" world.");
            } else if (!lab.robot().isEmpty()) {
                s.console.println("[mbot] Driving a real robot is not built yet. Using the simulator instead.");
            }
            s.goLive();
        }
        current.robotMade(robot);
        return current;
    }

    /** Begin a fresh headless run for a test. The next robot made joins it. */
    public static synchronized void startTesting(String worldName) {
        current = new Session(World.load(worldName), "Test", false);
    }

    /** The current run, for {@code mbot.Testing}. */
    public static synchronized Session testing() {
        if (current == null) throw new IllegalStateException("Call Testing.start(\"world\") first.");
        return current;
    }

    private synchronized void robotMade(Object made) {
        robots++;
        String kind = made.getClass().getSimpleName();
        if (robots > 1) {
            note("A second robot was made (new " + kind + "()). There is only one robot, so both names control it.");
            return;
        }
        robot = made;
        Map<String, Object> f = frame("start", "new " + kind + "()");
        boolean kit = made instanceof mbot.MBot2;
        if (kit) {
            parts.add("ultrasonic");
            parts.add("line");
        }
        f.put("parts", new ArrayList<>(parts));
        String where = " in the \"" + world.title() + "\" world.";
        if (Sources.isStudent(made.getClass().getName())) {
            // The student's own robot class: say what it inherits.
            StringBuilder chain = new StringBuilder();
            for (Class<?> c = made.getClass().getSuperclass(); c != null && c != Object.class; c = c.getSuperclass()) {
                chain.append(chain.length() == 0 ? "" : ", which is a ").append(c.getSimpleName());
            }
            add(f, "Your " + kind + " turned on" + where + " " + kind + " extends " + chain + ", so it can already "
                    + "do everything they can" + (kit ? ", and it comes with the kit's ultrasonic and line sensors." : "."));
        } else if (kit) {
            add(f, "The robot turned on" + where + " It comes with its ultrasonic sensor and line sensor plugged in.");
        } else {
            add(f, "A bare chassis turned on" + where + " It has wheels, motors and the CyberPi, but no sensors yet.");
        }
    }

    /** MBot2's own run() was called: nothing overrides it. */
    public synchronized void emptyRun() {
        Map<String, Object> f = frame("note", "run()");
        add(f, robot != null && Sources.isStudent(robot.getClass().getName())
                ? "run() was called, but " + robot.getClass().getSimpleName() + " does not override it, so MBot2's "
                        + "empty run() ran and nothing happened. Write public void run() in your class."
                : "MBot2's own run() does nothing. Write a class that extends MBot2 and override run().");
    }

    /** A sensor object was made and plugged into the robot. */
    public synchronized void attach(String part) {
        String name = PART_CLASS.get(part);
        Map<String, Object> f = frame("attach", "new " + name + "(...)");
        f.put("part", part);
        if (!parts.add(part)) {
            add(f, "The robot already has a" + (name.startsWith("U") ? "n " : " ") + name
                    + ". Both objects use the same one.");
            return;
        }
        add(f, switch (part) {
            case "ultrasonic" -> "Plugged an ultrasonic sensor into the front of the robot. Now it can tell how far away things are.";
            case "line" -> "Plugged a line sensor into the bottom of the robot. Now it can see tape on the floor.";
            case "scanner" -> "Mounted a scanner on top of the robot: an ultrasonic sensor on a servo, so it can look left and right.";
            default -> "Plugged a color sensor into the bottom of the robot. Now it can tell what color the floor is.";
        });
    }

    static final Map<String, String> PART_CLASS = Map.of(
            "ultrasonic", "Ultrasonic", "line", "LineSensor", "scanner", "Scanner", "color", "ColorSensor");

    private void goLive() {
        tee = new Tee(console, this::printed);
        System.setOut(tee.stream());
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            crashed(e);
            if (previous != null) {
                previous.uncaughtException(t, e);
            } else {
                System.err.print("Exception in thread \"" + t.getName() + "\" ");
                e.printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(this::finish, "mbot-save"));
    }

    // ------------------------------------------------------------------ commands

    public synchronized void straight(double cm) {
        Map<String, Object> f = frame("move", "straight(" + num(cm) + ")");
        sim.left = sim.right = 0;
        List<double[]> path = sim.straight(cm);
        extend(f, path);
        double[] a = path.get(0), b = path.get(path.size() - 1);
        double went = Math.hypot(b[0] - a[0], b[1] - a[1]);
        String dir = cm < 0 ? "backward" : "forward";
        if (sim.bumped) {
            f.put("bump", true);
            add(f, "Tried to drive " + dir + " " + num(Math.abs(cm)) + " cm, but bumped into a wall after "
                    + num(went) + " cm. The robot stopped there.");
        } else {
            add(f, "Drove " + dir + " " + num(Math.abs(cm)) + " cm.");
        }
    }

    public synchronized void turn(double degrees, String name) {
        Map<String, Object> f = frame("turn", name + "(" + num(Math.abs(degrees)) + ")");
        sim.left = sim.right = 0;
        extend(f, sim.turn(degrees));
        add(f, "Turned " + (degrees >= 0 ? "left " : "right ") + num(Math.abs(degrees)) + "°. Now facing "
                + compass(sim.h) + ".");
    }

    public synchronized void forward(double rpm, double seconds, String call) {
        Map<String, Object> f = frame("drive", call);
        double v = Specs.rpmToCmPerS(Math.max(-Specs.MAX_RPM, Math.min(Specs.MAX_RPM, rpm)));
        sim.left = sim.right = v;
        if (seconds > 0) {
            extend(f, sim.run(seconds));
            boolean bumped = sim.bumped;
            sim.left = sim.right = 0;
            if (bumped) f.put("bump", true);
            add(f, bumped ? "Drove forward, but bumped into a wall. The robot stopped."
                    : "Drove forward for " + num(seconds) + " s at speed " + num(rpm) + ", then stopped.");
        } else {
            add(f, "Started both wheels at speed " + num(rpm) + " (about " + num(v) + " cm/s). "
                    + "It keeps driving while the next lines of code run.");
        }
    }

    public synchronized void motorPower(double leftPower, double rightPower) {
        Map<String, Object> f = frame("drive", "setMotorPower(" + num(leftPower) + ", " + num(rightPower) + ")");
        sim.left = Specs.rpmToCmPerS(clamp(leftPower, 100) / 100 * Specs.MAX_RPM);
        sim.right = Specs.rpmToCmPerS(clamp(rightPower, 100) / 100 * Specs.MAX_RPM);
        String what = leftPower == rightPower ? (leftPower == 0 ? "stopped" : "straight")
                : leftPower == -rightPower ? "spinning on the spot"
                : leftPower < rightPower ? "curving left" : "curving right";
        add(f, "Set the wheel powers to left " + num(leftPower) + ", right " + num(rightPower) + ": " + what + ".");
    }

    public synchronized void stop() {
        Map<String, Object> f = frame("stop", "stop()");
        boolean was = sim.moving();
        sim.left = sim.right = 0;
        add(f, was ? "Stopped both wheels." : "Stopped (the wheels were already stopped).");
    }

    public synchronized double readUltrasonic(String call) {
        Map<String, Object> f = frame("read", call);
        reads++;
        double[] r = sim.ultrasonic();
        double cm = Math.round(r[0] * 10) / 10.0;
        f.put("v", cm);
        if (!Double.isNaN(r[1])) f.put("hit", new double[]{r[1], r[2]});
        add(f, cm >= Specs.US_MAX_CM ? "Read the ultrasonic sensor: nothing in range, so it returned 300."
                : "Read the ultrasonic sensor: " + num(cm) + " cm to the nearest thing ahead.");
        return cm;
    }

    public synchronized int readLineStatus(String call) {
        Map<String, Object> f = frame("read", call);
        reads++;
        int bits = sim.lineStatus();
        f.put("v", bits);
        f.put("line", true);
        StringBuilder probes = new StringBuilder();
        for (int i = 3; i >= 0; i--) probes.append((bits >> i & 1) == 1 ? '■' : '□');
        add(f, "Read the line sensor: " + bits + " " + probes + (bits == 0 ? " (no tape under it)." : "."));
        return bits;
    }

    public synchronized double[] scan(int[] angles) {
        Map<String, Object> f = frame("scan", "scan()");
        reads++;
        double[] out = new double[angles.length];
        List<double[]> rays = new ArrayList<>();
        for (int i = 0; i < angles.length; i++) {
            double[] r = sim.ultrasonicAt(angles[i], Specs.SCANNER_OFFSET_CM, Specs.SCANNER_HALF_ANGLE_DEG);
            out[i] = Math.round(r[0] * 10) / 10.0;
            rays.add(new double[]{angles[i], out[i], r[1], r[2]});
        }
        sim.time += Specs.SCAN_STEP_S * angles.length;
        f.put("rays", rays);
        f.put("v", out);
        int best = 0;
        for (int i = 1; i < out.length; i++) if (out[i] > out[best]) best = i;
        add(f, "Scanned " + angles.length + " directions, far left to far right. The most open way is "
                + dirName(angles[best]) + " (" + num(out[best]) + " cm).");
        return out;
    }

    public synchronized double look(int degrees) {
        int d = Math.max(-90, Math.min(90, degrees));
        Map<String, Object> f = frame("scan", "look(" + degrees + ")");
        reads++;
        double[] r = sim.ultrasonicAt(d, Specs.SCANNER_OFFSET_CM, Specs.SCANNER_HALF_ANGLE_DEG);
        double cm = Math.round(r[0] * 10) / 10.0;
        sim.time += Specs.SCAN_STEP_S;
        f.put("rays", List.of(new double[]{d, cm, r[1], r[2]}));
        f.put("v", cm);
        add(f, (d != degrees ? "The scanner only turns from -90 to 90, so it looked at " + d + ". " : "")
                + "Looked " + dirName(d) + ": " + num(cm) + " cm.");
        return cm;
    }

    public synchronized String readColor() {
        Map<String, Object> f = frame("read", "color()");
        reads++;
        String c = sim.colorUnder();
        f.put("v", c);
        f.put("color", c);
        add(f, "Read the color sensor: the floor is " + c + ".");
        return c;
    }

    static String dirName(int deg) {
        if (deg == 0) return "straight ahead";
        String side = deg > 0 ? "left" : "right";
        return Math.abs(deg) == 90 ? "to the " + side : Math.abs(deg) + "° to the " + side;
    }

    public synchronized void led(int id, int r, int g, int b) {
        Map<String, Object> f = frame("led", r == 0 && g == 0 && b == 0 ? "turnLedOff(" + id + ")"
                : "turnLedOn(" + id + ", " + r + ", " + g + ", " + b + ")");
        if (id < 1 || id > 5) {
            add(f, "There is no light " + id + ". The CyberPi's lights are numbered 1 to 5, so nothing happened.");
            return;
        }
        f.put("led", new double[]{id, clamp255(r), clamp255(g), clamp255(b)});
        add(f, r == 0 && g == 0 && b == 0 ? "Turned light " + id + " off." : "Turned light " + id + " on.");
    }

    // ------------------------------------------------------------------ recording

    /** Start a frame for a call. Time that passed since the last call is folded in first. */
    private Map<String, Object> frame(String kind, String call) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("k", kind);
        List<StackWalker.StackFrame> chain = sources.callers();
        if (!chain.isEmpty()) {
            f.put("f", chain.get(0).getFileName());
            f.put("l", chain.get(0).getLineNumber());
            // Who called the method that made this call: "called from Main.java line 11"
            List<Object> via = new ArrayList<>();
            for (int i = 1; i < chain.size(); i++) {
                via.add(List.of(chain.get(i).getFileName(), chain.get(i).getLineNumber()));
            }
            if (!via.isEmpty()) f.put("via", via);
        }
        f.put("c", call);
        String how = origin(call, chain);
        if (how != null) f.put("how", how);
        double wall = (System.nanoTime() - lastCallNanos) / 1e9;
        f.put("t0", sim.time);
        if (sim.moving()) {
            // The wheels were running while the student's code got here.
            extend(f, sim.run(Math.max(Specs.READ_TICK_S, wall)));
            if (sim.bumped) f.put("bump", true);
        } else if (kind.equals("read")) {
            sim.time += Specs.READ_TICK_S;
        } else if (wall > 0.2) {
            sim.time += wall;               // a Thread.sleep, or a slow loop: let the replay pause too
        }
        return f;
    }

    /**
     * For a student's own robot class: where the method being called comes from.
     * "turnLeft() is inherited from Chassis", or, inside an override, "super.straight() runs Chassis's version".
     */
    private String origin(String call, List<StackWalker.StackFrame> chain) {
        if (robot == null || !Sources.isStudent(robot.getClass().getName())) return null;
        int paren = call.indexOf('(');
        if (paren <= 0 || call.startsWith("new ")) return null;
        String name = call.substring(0, paren);
        Class<?> declaring = null;
        for (Class<?> c = robot.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (!Sources.isStudent(c.getName()) && declares(c, name)) {
                declaring = c;
                break;
            }
        }
        if (declaring == null) return null;           // not a robot method (a sensor's, for example)
        if (!chain.isEmpty() && chain.get(0).getMethodName().equals(name)
                && chain.get(0).getDeclaringClass().isInstance(robot)) {
            return "Inside " + chain.get(0).getDeclaringClass().getSimpleName() + "." + name + "(), super." + name
                    + "() runs " + declaring.getSimpleName() + "'s version.";
        }
        for (Class<?> c = robot.getClass(); c != declaring; c = c.getSuperclass()) {
            if (declares(c, name)) return null;       // overridden: the override's own lines are what run
        }
        return name + "() is inherited: " + robot.getClass().getSimpleName() + " gets it from "
                + declaring.getSimpleName() + ".";
    }

    private static boolean declares(Class<?> c, String name) {
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name) && !m.isSynthetic()) return true;
        }
        return false;
    }

    /** The student's robot class and everything above it, for the viewer's diagram. */
    private List<Object> robotDiagram() {
        if (robot == null) return null;
        List<Object> boxes = new ArrayList<>();
        for (Class<?> c = robot.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            Map<String, Object> box = new LinkedHashMap<>();
            boolean mine = Sources.isStudent(c.getName());
            box.put("name", c.getSimpleName());
            box.put("mine", mine);
            List<Object> fields = new ArrayList<>();
            List<Object> methods = new ArrayList<>();
            if (mine) {
                for (java.lang.reflect.Field fd : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(fd.getModifiers()) || fd.isSynthetic()) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", fd.getName());
                    m.put("type", fd.getType().getSimpleName());
                    String part = partOf(fd.getType());
                    if (part != null) {
                        m.put("part", part);
                        try {
                            fd.setAccessible(true);
                            m.put("set", fd.get(robot) != null);
                        } catch (ReflectiveOperationException | RuntimeException e) {
                            // leave it unknown
                        }
                    }
                    fields.add(m);
                }
                for (java.lang.reflect.Method md : c.getDeclaredMethods()) {
                    if (md.isSynthetic() || java.lang.reflect.Modifier.isStatic(md.getModifiers())) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", md.getName() + "()");
                    for (Class<?> up = c.getSuperclass(); up != null && up != Object.class; up = up.getSuperclass()) {
                        if (declares(up, md.getName())) {
                            m.put("overrides", up.getSimpleName());
                            break;
                        }
                    }
                    methods.add(m);
                }
            } else {
                for (java.lang.reflect.Method md : c.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isPublic(md.getModifiers())
                            && !methods.contains(md.getName() + "()")) methods.add(md.getName() + "()");
                }
                if (c == mbot.MBot2.class) fields.add(Map.of("name", "built in", "type", "Ultrasonic, LineSensor"));
            }
            box.put("fields", fields);
            box.put("methods", methods);
            boxes.add(box);
        }
        return boxes;
    }

    private static String partOf(Class<?> type) {
        if (type == mbot.Ultrasonic.class) return "ultrasonic";
        if (type == mbot.LineSensor.class) return "line";
        if (type == mbot.Scanner.class) return "scanner";
        if (type == mbot.ColorSensor.class) return "color";
        return null;
    }

    @SuppressWarnings("unchecked")
    private void extend(Map<String, Object> f, List<double[]> path) {
        List<double[]> p = (List<double[]>) f.computeIfAbsent("p", k -> new ArrayList<double[]>());
        for (double[] pt : path) {
            if (!p.isEmpty()) {
                double[] last = p.get(p.size() - 1);
                if (last[0] == pt[0] && last[1] == pt[1] && last[2] == pt[2]) continue;
            }
            p.add(pt);
        }
    }

    private void add(Map<String, Object> f, String message) {
        if (Boolean.TRUE.equals(f.get("bump"))) bumps++;
        if (Boolean.TRUE.equals(f.get("bump")) && !message.contains("bump")) {
            message = "Bumped into a wall while driving, so the wheels stopped. Then: " + lower(message);
        }
        f.put("m", message);
        f.put("t1", sim.time);
        f.put("pose", new double[]{sim.x, sim.y, sim.h});
        frames.add(f);
        lastCallNanos = System.nanoTime();
        checkLimits();
    }

    private void printed(String text) {
        synchronized (this) {
            if (finished) return;
            Map<String, Object> f = frame("print", "System.out.println");
            f.put("out", text);
            add(f, "Printed: " + text);
        }
    }

    private void note(String text) {
        Map<String, Object> f = frame("note", "");
        add(f, text);
    }

    private synchronized void crashed(Throwable e) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("k", "error");
        for (StackTraceElement el : e.getStackTrace()) {
            if (Sources.isStudent(el.getClassName())) {
                sources.remember(el);
                f.put("f", el.getFileName());
                f.put("l", el.getLineNumber());
                break;
            }
        }
        f.put("t0", sim.time);
        f.put("c", e.getClass().getSimpleName());
        String msg = e.getMessage() == null ? "" : ": " + e.getMessage();
        String help = explain(e);
        add(f, help != null
                ? "The program crashed with a " + e.getClass().getSimpleName() + ", so the robot stopped. " + help
                : "The program crashed with " + e.getClass().getSimpleName() + msg + ". The robot stopped. "
                        + "Check the red line, and the console in IntelliJ for details.");
    }

    private static final Pattern NULL_NAME = Pattern.compile("because \"(?:this\\.)?([\\w.]+)\" is null");

    /** Plain English for the crashes beginners hit most. */
    static String explain(Throwable e) {
        if (e instanceof NullPointerException && e.getMessage() != null) {
            Matcher m = NULL_NAME.matcher(e.getMessage());
            if (m.find() && !m.group(1).startsWith("<")) {
                String name = m.group(1);
                return "\"" + name + "\" is null: it was declared, but never given an object. "
                        + "Is there a line like " + name + " = new ...; in the constructor, and does it run first?";
            }
            return "Something was null: a variable that was declared but never given an object with new.";
        }
        return null;
    }

    private void checkLimits() {
        if (finished || !live) return;
        String why = null;
        if (sim.time > Specs.MAX_SIM_SECONDS) {
            why = "The program ran for 10 minutes of robot time, so the simulator stopped it. "
                    + "Is there a loop that never ends?";
        } else if (frames.size() > Specs.MAX_FRAMES) {
            why = "The program made " + Specs.MAX_FRAMES + " robot calls, so the simulator stopped it. "
                    + "Is there a loop that never ends?";
        }
        if (why != null) {
            Map<String, Object> f = frame("error", "");
            f.put("m", why);
            f.put("t1", sim.time);
            frames.add(f);
            finish();
            console.println("[mbot] " + why);
            System.exit(1);
        }
    }

    /** Save the run and bring up the viewer. Runs once, from the shutdown hook or a limit. */
    void finish() {
        // Not synchronized up front: a limit calls System.exit while holding the lock, and the
        // shutdown hook must see "finished" and return instead of waiting for that lock forever.
        if (finished) return;
        synchronized (this) {
            if (finished) return;
            if (tee != null) tee.finish();
            finished = true;
            save();
        }
        if (live) Viewer.show(console);
    }

    private List<Object> diagram;

    @SuppressWarnings("unchecked")
    private void save() {
        boolean crashed = !frames.isEmpty() && "error".equals(frames.get(frames.size() - 1).get("k"));
        diagram = robotDiagram();
        if (diagram != null && !diagram.isEmpty()) {
            Map<String, Object> top = (Map<String, Object>) diagram.get(0);
            for (Object o : (List<Object>) top.get("fields")) {
                Map<String, Object> fd = (Map<String, Object>) o;
                if (Boolean.FALSE.equals(fd.get("set"))) {
                    Map<String, Object> f = frame("note", "");
                    add(f, top.get("name") + " declares " + fd.get("type") + " " + fd.get("name") + ", but it is still null: "
                            + "it was never plugged in. Add " + fd.get("name") + " = new " + fd.get("type")
                            + "(this); to the constructor.");
                }
            }
        }
        for (String need : world.requires) {
            if (!parts.contains(need) && robots > 0) {
                Map<String, Object> f = frame("note", "");
                add(f, "This world is built for a" + (need.equals("ultrasonic") ? "n " : " ") + PART_CLASS.get(need)
                        + ", but your robot never got one. Plug one in: new " + PART_CLASS.get(need) + "(robot)");
            }
        }
        if (!crashed) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("k", "end");
            f.put("t0", sim.time);
            f.put("c", "");
            f.put("m", sim.moving() ? "The program ended, so the robot stopped. (Its wheels were still on.)"
                    : "The program ended.");
            f.put("t1", sim.time);
            f.put("pose", new double[]{sim.x, sim.y, sim.h});
            frames.add(f);
        }
        if (!live) return;
        try {
            write();
        } catch (IOException e) {
            console.println("[mbot] Could not save the run: " + e.getMessage());
        }
    }

    public Map<String, Object> trace() {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("format", 1);
        t.put("id", Long.toString(System.currentTimeMillis(), 36) + Long.toString((long) (Math.random() * 1e9), 36));
        t.put("created", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        t.put("main", mainName);
        t.put("worldName", world.name);
        t.put("world", world.raw);
        Map<String, Object> specs = new LinkedHashMap<>();
        specs.put("radius", Specs.RADIUS_CM);
        specs.put("usOffset", Specs.US_OFFSET_CM);
        specs.put("usHalfAngle", Specs.US_HALF_ANGLE_DEG);
        specs.put("usMax", Specs.US_MAX_CM);
        specs.put("lineAhead", Specs.LINE_AHEAD_CM);
        specs.put("lineProbes", Specs.LINE_PROBES_CM);
        specs.put("scannerOffset", Specs.SCANNER_OFFSET_CM);
        t.put("robot", specs);
        if (diagram != null) t.put("diagram", diagram);
        t.put("sources", sources.files);
        t.put("frames", frames);
        return t;
    }

    private Path write() throws IOException {
        Path dir = Viewer.home().resolve("runs");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String json = Json.write(trace());
        Path run = dir.resolve(stamp + "-" + mainName + ".json");
        Files.writeString(run, json, StandardCharsets.UTF_8);
        Path tmp = Viewer.home().resolve("latest.json.tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, Viewer.home().resolve("latest.json"), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        prune(dir);
        return run;
    }

    private static void prune(Path dir) {
        try (var s = Files.list(dir)) {
            List<Path> runs = s.sorted().toList();
            for (int i = 0; i < runs.size() - 50; i++) Files.deleteIfExists(runs.get(i));
        } catch (IOException ignored) {
            // old runs are a convenience
        }
    }

    // ------------------------------------------------------------------ for mbot.Testing

    public synchronized double x() { return sim.x; }
    public synchronized double y() { return sim.y; }
    public synchronized double heading() { return sim.h; }
    public synchronized int bumps() { return bumps; }
    public synchronized int reads() { return reads; }
    public synchronized double time() { return sim.time; }
    public synchronized Set<String> parts() { return Set.copyOf(parts); }

    public synchronized boolean insideZone(String label) {
        for (double[] z : world.zones(label)) {
            if (sim.x >= z[0] && sim.x <= z[0] + z[2] && sim.y >= z[1] && sim.y <= z[1] + z[3]) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    public static String num(double d) {
        double r = Math.round(d * 10) / 10.0;
        return r == Math.rint(r) ? Long.toString((long) r) : Double.toString(r);
    }

    static String compass(double h) {
        String[] names = {"right (east)", "up-right", "up (north)", "up-left", "left (west)", "down-left",
                "down (south)", "down-right"};
        return names[(int) Math.round(Sim.norm(h) / 45) % 8];
    }

    private static double clamp(double v, double lim) {
        return Math.max(-lim, Math.min(lim, v));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String lower(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
