package mbot.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds which line of the student's code made each call, and reads their source files so
 * the viewer can show the code next to the robot.
 */
final class Sources {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /** File name (e.g. "Main.java") -> its text. Insertion order = order first seen. */
    final Map<String, String> files = new LinkedHashMap<>();
    private final Path root = Path.of(System.getProperty("user.dir"));

    /** Library and JDK code is never "the student's line". */
    static boolean isStudent(String className) {
        return !(className.startsWith("mbot.") || className.startsWith("java.") || className.startsWith("javax.")
                || className.startsWith("jdk.") || className.startsWith("sun.") || className.startsWith("com.sun.")
                || className.startsWith("com.intellij.") || className.startsWith("org.junit."));
    }

    /** The innermost frame in student code right now, or null. */
    StackWalker.StackFrame caller() {
        Optional<StackWalker.StackFrame> f = WALKER.walk(s -> s.filter(fr -> isStudent(fr.getClassName())).findFirst());
        f.ifPresent(this::remember);
        return f.orElse(null);
    }

    /** The class whose main() started the program: the outermost frame. */
    static Class<?> mainClass() {
        return WALKER.walk(s -> s.reduce((a, b) -> b)).map(StackWalker.StackFrame::getDeclaringClass).orElse(null);
    }

    void remember(StackWalker.StackFrame f) {
        remember(f.getClassName(), f.getFileName());
    }

    void remember(StackTraceElement e) {
        remember(e.getClassName(), e.getFileName());
    }

    private void remember(String className, String fileName) {
        if (fileName == null || files.containsKey(fileName)) return;
        String text = read(className, fileName);
        if (text != null) files.put(fileName, text);
    }

    private String read(String className, String fileName) {
        int dot = className.lastIndexOf('.');
        String pkgPath = dot < 0 ? "" : className.substring(0, dot).replace('.', '/') + "/";
        for (String base : new String[]{"src/main/java/", "src/", ""}) {
            Path p = root.resolve(base + pkgPath + fileName);
            if (Files.isRegularFile(p)) return slurp(p);
        }
        // Unusual layout: look for it, but not forever.
        try (Stream<Path> walk = Files.walk(root, 8)) {
            return walk.filter(p -> p.getFileName().toString().equals(fileName))
                    .filter(p -> !p.toString().contains("/target/") && !p.toString().contains("/.git/"))
                    .findFirst().map(Sources::slurp).orElse(null);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String slurp(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
