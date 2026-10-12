package mbot.internal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * The viewer server: a background JVM started by the first run. Serves the page and
 * three.js out of the jar, and the latest saved run out of ~/.mbot-sim.
 *
 *   GET /                 the viewer page
 *   GET /vendor/...       three.js, bundled (works with no internet)
 *   GET /api/ping         {"app":"mbot-sim","watching":N}   N = tabs that polled recently
 *   GET /api/latest/id    the id of the latest run, so tabs can poll cheaply
 *   GET /api/latest       the latest run's trace
 *
 * Only listens on 127.0.0.1. Quits by itself after 30 minutes with no tab open.
 */
public final class ViewerMain {

    private static final long IDLE_EXIT_MS = 30 * 60 * 1000L;
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8", "txt", "text/plain; charset=utf-8");

    private static final long STARTED = System.currentTimeMillis();
    private static volatile long lastPoll;   // 0 until a tab polls: a fresh server has nobody watching

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Viewer.PORT;
        Path latest = Viewer.home().resolve("latest.json");
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "mbot-viewer");
            t.setDaemon(true);
            return t;
        }));

        server.createContext("/api/ping", ex -> {
            int watching = System.currentTimeMillis() - lastPoll < 4000 ? 1 : 0;
            send(ex, 200, "json", ("{\"app\":\"mbot-sim\",\"watching\":" + watching + "}").getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/api/latest/id", ex -> {
            lastPoll = System.currentTimeMillis();
            String id = "";
            if (Files.exists(latest)) {
                // The id is near the top of the file; no need to parse the whole trace.
                String head = new String(readHead(latest), StandardCharsets.UTF_8);
                int i = head.indexOf("\"id\":\"");
                if (i >= 0) id = head.substring(i + 6, head.indexOf('"', i + 6));
            }
            send(ex, 200, "txt", id.getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/api/latest", ex -> {
            if (!Files.exists(latest)) {
                send(ex, 404, "txt", "no runs yet".getBytes(StandardCharsets.UTF_8));
                return;
            }
            send(ex, 200, "json", Files.readAllBytes(latest));
        });
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            if (path.contains("..")) {
                send(ex, 404, "txt", new byte[0]);
                return;
            }
            try (InputStream in = ViewerMain.class.getResourceAsStream("/mbot/viewer" + path)) {
                if (in == null) {
                    send(ex, 404, "txt", ("not found: " + path).getBytes(StandardCharsets.UTF_8));
                    return;
                }
                String ext = path.substring(path.lastIndexOf('.') + 1);
                send(ex, 200, ext, in.readAllBytes());
            }
        });
        server.start();
        System.out.println("mbot-sim viewer on http://localhost:" + port + "/  (runs in " + Viewer.home() + ")");

        // Quit when nobody has looked for a while, so it never lingers all week.
        while (true) {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                break;
            }
            if (System.currentTimeMillis() - Math.max(lastPoll, STARTED) > IDLE_EXIT_MS) break;
        }
        server.stop(0);
        System.exit(0);
    }

    private static byte[] readHead(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            return in.readNBytes(256);
        }
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(type, "application/octet-stream"));
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length == 0 ? -1 : body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
