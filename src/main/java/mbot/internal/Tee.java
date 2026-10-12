package mbot.internal;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Wraps System.out so every println still reaches the console, and each finished line is
 * also handed to the recorder. Students see their output in the viewer next to the
 * line of code that printed it.
 */
final class Tee extends OutputStream {

    private final PrintStream original;
    private final Consumer<String> onLine;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();

    Tee(PrintStream original, Consumer<String> onLine) {
        this.original = original;
        this.onLine = onLine;
    }

    PrintStream stream() {
        return new PrintStream(this, true, StandardCharsets.UTF_8);
    }

    @Override
    public synchronized void write(int b) {
        original.write(b);
        if (b == '\n') {
            emit();
        } else if (b != '\r') {
            pending.write(b);
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        for (int i = off; i < off + len; i++) write(b[i]);
    }

    @Override
    public synchronized void flush() {
        original.flush();
    }

    private void emit() {
        String line = pending.toString(StandardCharsets.UTF_8);
        pending.reset();
        onLine.accept(line);
    }

    /** Hand over a last line that was printed without a newline. */
    synchronized void finish() {
        if (pending.size() > 0) emit();
    }
}
