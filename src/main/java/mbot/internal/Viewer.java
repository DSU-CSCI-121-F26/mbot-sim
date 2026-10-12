package mbot.internal;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The student program's side of the viewer: make sure the viewer server is running
 * (start it in the background if not), and open the browser if no tab is watching.
 *
 * The server is a separate JVM so it outlives the student's program. It is started from
 * the same classpath, so it is always the same version as the library that saved the run.
 */
final class Viewer {

    static final int PORT = Integer.getInteger("mbot.port", 8121);

    private Viewer() {}

    /** ~/.mbot-sim: saved runs live here, never in the student's repo. */
    static Path home() {
        String override = System.getProperty("mbot.home");
        return override != null ? Path.of(override) : Path.of(System.getProperty("user.home"), ".mbot-sim");
    }

    static String url() {
        return "http://localhost:" + PORT + "/";
    }

    static void show(PrintStream console) {
        if ("off".equals(System.getProperty("mbot.viewer"))) return;
        String status = ping();
        if (status == null) {
            if (!launch(console)) return;
            for (int i = 0; i < 40 && status == null; i++) {
                sleep(100);
                status = ping();
            }
            if (status == null) {
                console.println("[mbot] The viewer did not start. See " + home().resolve("viewer.log"));
                return;
            }
        }
        // The ping reports how many tabs polled in the last few seconds. None: open one.
        if (status.contains("\"watching\":0") && !"off".equals(System.getProperty("mbot.open"))) {
            open(console);
        }
        console.println("[mbot] Replay in your browser: " + url());
    }

    /** null if nothing answers on the port. */
    static String ping() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url() + "api/ping").openConnection();
            c.setConnectTimeout(300);
            c.setReadTimeout(500);
            try (InputStream in = c.getInputStream()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return body.contains("mbot-sim") ? body : null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean launch(PrintStream console) {
        String java = ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        List<String> cmd = new ArrayList<>(List.of(java, "-cp", System.getProperty("java.class.path"),
                "-Dmbot.home=" + home(), "-Djava.awt.headless=true", ViewerMain.class.getName(), Integer.toString(PORT)));
        try {
            File log = home().resolve("viewer.log").toFile();
            log.getParentFile().mkdirs();
            new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log)
                    .redirectInput(ProcessBuilder.Redirect.PIPE).start().getOutputStream().close();
            return true;
        } catch (IOException e) {
            console.println("[mbot] Could not start the viewer: " + e.getMessage());
            return false;
        }
    }

    private static void open(PrintStream console) {
        String url = url();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception | Error ignored) {
            // fall through to the OS command
        }
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String[] cmd = os.contains("mac") ? new String[]{"open", url}
                : os.contains("win") ? new String[]{"rundll32", "url.dll,FileProtocolHandler", url}
                : new String[]{"xdg-open", url};
        try {
            new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            console.println("[mbot] Open this in your browser: " + url);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
