package mbot.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplainTest {

    static class Rover {
        mbot.Ultrasonic eyes;

        double look() {
            return eyes.distance();
        }
    }

    @Test
    void namesTheNullField() {
        Session.startTesting("open-room");
        try {
            new Rover().look();
        } catch (NullPointerException e) {
            String help = Session.explain(e);
            assertTrue(help.startsWith("\"eyes\" is null"), help);
            return;
        }
        throw new AssertionError("expected a NullPointerException");
    }
}
