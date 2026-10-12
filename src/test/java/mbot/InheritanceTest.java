package mbot;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A student's own robot that extends MBot2, and the two extra sensors. */
class InheritanceTest {

    /** What a student would write. */
    static class Scout extends MBot2 {
        Scanner scanner;
        ColorSensor eye;
        int carefulStops;

        Scout() {
            super();
            scanner = new Scanner(this);
            eye = new ColorSensor(this);
        }

        @Override
        public void straight(double cm) {       // a careful bot: never drives into something
            if (readUltrasonic() > cm + 10) {
                super.straight(cm);
            } else {
                carefulStops++;
            }
        }

        @Override
        public void run() {
            while (!eye.color().equals("red")) {
                straight(5);
            }
        }
    }

    @Test
    void aSubclassGetsTheKitSensorsPlusItsOwn() {
        Testing.start("open-room");
        new Scout();
        assertEquals(Set.of("ultrasonic", "line", "scanner", "color"), Testing.parts());
    }

    @Test
    void polymorphismRunsTheOverride() {
        Testing.start("color-zones");
        MBot2 bot = new Scout();
        bot.run();                                  // MBot2's run() is empty: this must run Scout's
        // It stops as soon as its sensor, 6 cm ahead of the centre, reaches the pad at y = 240
        assertEquals("red", ((Scout) bot).eye.color());
        assertEquals(235, Testing.y(), 1e-9);
    }

    @Test
    void theOverrideCanRefuse() {
        Testing.start("wall-ahead");                // wall 153 cm ahead of the sensor
        Scout s = new Scout();
        s.straight(200);
        assertEquals(1, s.carefulStops);
        assertEquals(40, Testing.y(), 1e-9);
        assertEquals(0, Testing.bumps());
    }

    @Test
    void colorsUnderTheSensor() {
        Testing.start("color-zones");               // sensor 6 cm ahead of centre (start y = 30)
        Scout s = new Scout();
        assertEquals("white", s.eye.color());
        s.straight(50);                             // sensor at y = 86: green stripe 80-100
        assertEquals("green", s.eye.color());
        s.straight(70);                             // y = 156: blue stripe 150-170
        assertEquals("blue", s.eye.color());
    }

    @Test
    void scanSeesTheJunction() {
        Testing.start("junctions");
        Scout s = new Scout();
        // Start (40, 30) facing north. Scanner 3 cm ahead of the centre, at (40, 33).
        double[] d = s.scanner.scan();
        assertEquals(7, d.length);
        assertEquals(40, d[0], 0.6);                // far left: the room's wall at x = 0
        assertEquals(40, d[6], 0.6);                // far right: the block at x = 80
        assertEquals(157, d[3], 0.6);               // ahead: the upper block at y = 190
        // Drive to the junction, then the open way is to the right.
        s.straight(130);                            // centre at (40, 160)
        d = s.scanner.scan();
        assertTrue(d[6] > 200 && d[0] < 50, java.util.Arrays.toString(d));
    }

    @Test
    void lookAimsOneWay() {
        Testing.start("junctions");
        Scout s = new Scout();
        assertEquals(40, s.scanner.look(90), 0.6);
        assertArrayEquals(new int[]{90, 60, 30, 0, -30, -60, -90}, Scanner.ANGLES);
    }
}
