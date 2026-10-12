package mbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Student-built boards, built-in and student-written drones, tags and shutdown. */
class BoardAndDroneTest {

    /** A corridor: start at the bottom, goal at the top. Each cell is 20 cm. */
    public static class Corridor extends Board {
        public Corridor() {
            super("Corridor", "test");
            map(
                "#####",
                "#.G.#",
                "#...#",
                "#...#",
                "#.S.#",
                "#####"
            );
        }
    }

    @Test
    void aMapBecomesABoard() {
        Testing.start(Corridor.class);
        MBot2 bot = new MBot2();
        assertEquals(50, Testing.x(), 1e-9);            // S is column 2: centre 2.5 cells = 50 cm
        assertEquals(30, Testing.y(), 1e-9);            // row 5 of 6 from the top = 1 cell up: centre 30 cm
        assertEquals(90, Testing.heading(), 1e-9);
        // wall face at the top row's bottom edge: y = 100; the sensor is 7 cm ahead of y = 30
        assertEquals(63, bot.readUltrasonic(), 0.01);
        bot.straight(60);                               // centre at y = 90: inside G (y 80-100)
        assertTrue(Testing.passed(), Testing.why().toString());
    }

    @Test
    void notReachingTheGoalFails() {
        Testing.start(Corridor.class);
        new MBot2().straight(10);
        assertFalse(Testing.passed());
        assertEquals("[it did not finish in the Goal]", Testing.why().toString());
    }

    @Test
    void aBadMapCharacterIsExplained() {
        class Bad extends Board {
            Bad() {
                super("Bad", "test");
                map("#S?#");
            }
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Bad());
        assertTrue(e.getMessage().contains("'?'"), e.getMessage());
    }

    /** A drone that hovers and stares the way it was placed. */
    public static class Starer extends Drone {
        @Override
        public void tick() {
            setSpeed(0);
            setTurnRate(0);
        }
    }

    /** Open floor; a drone 80 cm up the board, facing down at the robot. */
    public static class Watched extends Board {
        public Watched() {
            super("Watched", "test");
            size(200, 200);
            start(100, 40, 90);
            add(new Starer(), 100, 120, 270);
        }
    }

    @Test
    void stayingInSightGetsYouTaggedAndThreeTagsShutYouDown() {
        Testing.start(Watched.class);
        MBot2 bot = new MBot2();
        Testing.run(() -> {
            for (int i = 0; i < 1000; i++) bot.readUltrasonic();    // 0.02 s each: sit still for up to 20 s
        });
        assertEquals(3, Testing.tags());
        assertTrue(Testing.shutDown());
        assertFalse(Testing.passed());
        // first tag after 0.5 s in sight; it keeps watching while it recharges (2 s), so the next
        // tags land at 2.5 s and 4.5 s
        assertEquals(4.5, Testing.time(), 0.05);
    }

    @Test
    void aShutDownRobotRefusesCommands() {
        Testing.start(Watched.class);
        MBot2 bot = new MBot2();
        Testing.run(() -> {
            for (int i = 0; i < 1000; i++) bot.readUltrasonic();
        });
        assertThrows(RobotShutDownException.class, () -> bot.straight(10));
    }

    /** The same drone, with a wall between it and the robot. */
    public static class Hidden extends Board {
        public Hidden() {
            super("Hidden", "test");
            size(200, 200);
            start(100, 40, 90);
            wall(60, 70, 80, 10);
            add(new Starer(), 100, 120, 270);
        }
    }

    @Test
    void wallsBlockTheView() {
        Testing.start(Hidden.class);
        MBot2 bot = new MBot2();
        for (int i = 0; i < 500; i++) bot.readUltrasonic();          // 10 s
        assertEquals(0, Testing.tags());
    }

    @Test
    void theRadarKnowsWhenYouAreWatched() {
        Testing.start(Watched.class);
        Chassis base = new Chassis();
        DroneRadar radar = new DroneRadar(base);
        assertTrue(radar.watched());
        assertEquals(80, radar.nearestDistance(), 0.01);
        assertEquals(0, radar.nearestAngle(), 0.01);                  // straight ahead
        base.turnLeft(90);                                            // now the drone is to the right
        assertEquals(-90, radar.nearestAngle(), 0.01);
    }

    public static class HunterRoom extends Board {
        public HunterRoom() {
            super("Hunter room", "test");
            size(300, 300);
            start(150, 40, 90);
            hunter(150, 200);
        }
    }

    @Test
    void aHunterFindsAStillRobot() {
        Testing.start(HunterRoom.class);
        MBot2 bot = new MBot2();
        Testing.run(() -> {
            for (int i = 0; i < 1500; i++) bot.readUltrasonic();    // up to 30 s
        });
        assertTrue(Testing.shutDown(), "tags: " + Testing.tags());
    }

    public static class Timed extends Board {
        public Timed() {
            super("Timed", "test");
            size(100, 400);
            start(50, 20, 90);
            timeLimit(5);
        }
    }

    @Test
    void runningOutOfTimeShutsTheRobotDown() {
        Testing.start(Timed.class);
        MBot2 bot = new MBot2();
        Testing.run(() -> bot.straight(200));                       // 13 s at 15 cm/s
        assertTrue(Testing.shutDown());
        assertEquals(5, Testing.time(), 0.02);
        assertEquals(20 + 5 * 15, Testing.y(), 0.3);
        assertEquals("[out of time]", Testing.why().toString());
    }

    @Test
    void aPatrolFliesItsLoop() {
        class Loop extends Board {
            final Patrol p = new Patrol(50, 50, 250, 50);
            Loop() {
                super("Loop", "test");
                size(300, 300);
                start(150, 250, 90);
                add(p, 50, 50, 0);
            }
        }
        Testing.start("open-room");
        Loop loop = new Loop();
        assertEquals(50, loop.p.start()[0], 1e-9);
    }

    @Test
    void aRunawayLoopIsStoppedInTests() {
        Testing.start("open-room");
        MBot2 bot = new MBot2();
        Testing.run(() -> {
            while (true) {
                bot.readUltrasonic();      // never ends on its own
            }
        });
        assertTrue(Testing.shutDown());
        assertEquals("[ran too long]", Testing.why().toString());
    }

    @Test
    void aBoardObjectCanBeUsedDirectly() {
        Testing.start(new Corridor());
        new MBot2().straight(60);
        assertTrue(Testing.passed());
    }

    @Test
    void theRadarSaysWhichWayTheDroneIsLooking() {
        Testing.start(Watched.class);                 // the drone faces down, straight at the robot
        DroneRadar radar = new DroneRadar(new Chassis());
        assertEquals(0, radar.nearestLookAngle(), 0.01);
    }
}
