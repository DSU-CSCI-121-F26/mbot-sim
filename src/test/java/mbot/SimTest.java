package mbot;

import mbot.internal.Sim;
import mbot.internal.Specs;
import mbot.internal.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The physics, checked against numbers you can work out by hand. */
class SimTest {

    @Test
    void aSquareEndsWhereItStarted() {
        Sim s = new Sim(World.load("open-room"));
        for (int i = 0; i < 4; i++) {
            s.straight(30);
            s.turn(90);
        }
        assertEquals(120, s.x, 1e-6);
        assertEquals(60, s.y, 1e-6);
        assertEquals(90, s.h, 1e-6);
        assertEquals(4 * 30 / Specs.STRAIGHT_CM_PER_S + 4 * 90 / Specs.TURN_DEG_PER_S, s.time, 1e-6);
    }

    @Test
    void theWallStopsTheRobotAtItsBumper() {
        Sim s = new Sim(World.load("wall-ahead"));     // start y = 40, wall face at y = 200
        s.straight(500);
        assertTrue(s.bumped);
        assertEquals(200 - Specs.RADIUS_CM, s.y, 0.51);
        assertEquals(100, s.x, 1e-6);
    }

    @Test
    void ultrasonicMeasuresFromTheSensorNotTheCentre() {
        Sim s = new Sim(World.load("wall-ahead"));
        // sensor at y = 40 + 7 = 47; wall face at y = 200
        assertEquals(153, s.ultrasonic()[0], 0.01);
        s.turn(180);
        // facing the back wall at y = 0: sensor at y = 33
        assertEquals(33, s.ultrasonic()[0], 0.01);
    }

    @Test
    void measuresToTheHallwayBlock() {
        Sim s = new Sim(World.load("hallway"));        // 37,30 facing east: block at x = 75
        s.turn(-90);
        assertEquals(75 - 37 - Specs.US_OFFSET_CM, s.ultrasonic()[0], 0.01);
    }

    @Test
    void lineSensorSeesTheParkingBoxEdge() {
        Sim s = new Sim(World.load("parking"));
        assertEquals(0, s.lineStatus());
        // probes are 6 cm ahead of the centre; the box's near edge is the line y = 170
        s.straight(170 - 40 - Specs.LINE_AHEAD_CM);
        assertEquals(15, s.lineStatus());
    }

    @Test
    void forwardRunsUntilTheWall() {
        Sim s = new Sim(World.load("wall-ahead"));
        s.left = s.right = Specs.rpmToCmPerS(50);
        s.run(60);
        assertTrue(s.bumped);
        assertFalse(s.moving());
        assertEquals(200 - Specs.RADIUS_CM, s.y, 0.5);
    }

    @Test
    void unequalWheelsCurveLeft() {
        Sim s = new Sim(World.load("open-room"));
        s.left = 5;
        s.right = 10;
        s.run(1);
        assertTrue(s.h > 90, "right wheel faster should turn the robot left (counter-clockwise)");
    }
}
