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

    @Test
    void sensorBeforeRobotIsExplainedInItsOwnWords() {
        try {
            new mbot.Ultrasonic(null);
        } catch (IllegalArgumentException e) {
            String help = Session.explain(e);
            assertTrue(help.startsWith("An Ultrasonic needs a robot to plug into"), help);
            return;
        }
        throw new AssertionError("expected an IllegalArgumentException");
    }

    @Test
    void endlessRecursionGetsAHint() {
        assertTrue(Session.explain(new StackOverflowError()).contains("super.straight(cm)"));
    }
}
