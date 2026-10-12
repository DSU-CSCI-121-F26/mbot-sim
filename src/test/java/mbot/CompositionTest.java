package mbot;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Building a robot from parts, and the worlds that need them. */
class CompositionTest {

    @Test
    void aBareChassisHasNoSensorsUntilTheyArePluggedIn() {
        Testing.start("open-room");
        Chassis base = new Chassis();
        assertEquals(Set.of(), Testing.parts());
        new Ultrasonic(base);
        assertEquals(Set.of("ultrasonic"), Testing.parts());
        new LineSensor(base);
        assertEquals(Set.of("ultrasonic", "line"), Testing.parts());
    }

    @Test
    void anMBot2ComesWithBoth() {
        Testing.start("open-room");
        new MBot2();
        assertEquals(Set.of("ultrasonic", "line"), Testing.parts());
    }

    @Test
    void aSensorNeedsARobot() {
        Testing.start("open-room");
        assertThrows(IllegalArgumentException.class, () -> new Ultrasonic(null));
    }

    @Test
    void theUltrasonicSeesTheFirstBox() {
        Testing.start("obstacles");
        Ultrasonic eyes = new Ultrasonic(new Chassis());
        // sensor at y = 25 + 7 = 32; the box's near face is at y = 90
        assertEquals(58, eyes.distance(), 0.01);
    }

    @Test
    void cansAreSolidAndVisible() {
        Testing.start("obstacles");
        Chassis base = new Chassis();
        Ultrasonic eyes = new Ultrasonic(base);
        base.turnLeft(90);
        base.straight(80);                  // now at (70, 25)
        base.turnRight(90);                 // facing north, straight at the red can (70, 165), radius 6
        // sensor at y = 32; the can's near edge is at y = 159
        assertEquals(127, eyes.distance(), 0.01);
        base.straight(200);
        assertEquals(1, Testing.bumps());
        assertEquals(165 - 6 - 8.5, Testing.y(), 0.6);     // stopped with its bumper on the can
    }

    @Test
    void avoidingTheBoxReachesTheGoal() {
        Testing.start("obstacles");
        Chassis base = new Chassis();
        Ultrasonic eyes = new Ultrasonic(base);
        // A plan a student could write: up to the box, round its left side, up to the far wall, right into the goal.
        while (eyes.distance() > 15) base.straight(5);
        base.turnLeft(90);
        base.straight(60);
        base.turnRight(90);
        while (eyes.distance() > 15) base.straight(5);
        base.turnRight(90);
        base.straight(50);
        assertTrue(Testing.insideZone("Goal"), "ended at " + Testing.x() + ", " + Testing.y());
        assertEquals(0, Testing.bumps());
    }

    @Test
    void aSimpleLineFollowerCompletesALap() {
        Testing.start("line-track");
        Chassis base = new Chassis();
        LineSensor floor = new LineSensor(base);
        boolean leftTheStart = false;
        for (int step = 0; step < 4000; step++) {
            int s = floor.status();
            if ((s & 0b1000) != 0 || s == 0b0100) {
                base.turnLeft(4);
            } else if ((s & 0b0001) != 0 || s == 0b0010) {
                base.turnRight(4);
            } else if (s == 0) {
                base.turnLeft(4);           // lost it: the track only bends left
            } else {
                base.straight(1);
            }
            if (!Testing.insideZone("Lap") && Testing.x() > 200) leftTheStart = true;
            if (leftTheStart && Testing.insideZone("Lap")) break;
        }
        assertTrue(leftTheStart && Testing.insideZone("Lap"), "did not get round: ended at "
                + Testing.x() + ", " + Testing.y());
        assertEquals(0, Testing.bumps());
    }
}
