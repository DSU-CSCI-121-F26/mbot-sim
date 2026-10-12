package mbot.internal;

import mbot.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardCheckTest {

    public static class Blocked extends Board {
        public Blocked() {
            super("Blocked", "test");
            map(
                "#####",
                "#.G.#",
                "#####",
                "#.S.#",
                "#####"
            );
        }
    }

    public static class Narrow extends Board {
        public Narrow() {
            super("Narrow", "test");
            map(
                "###",
                "#G#",
                "#.#",
                "#S#",
                "###"
            );
        }
    }

    @Test
    void aSealedOffGoalIsReported() {
        var problems = BoardCheck.problems(World.fromBoard(Blocked.class));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith("the Goal cannot be reached"), problems.get(0));
    }

    @Test
    void aOneCellCorridorIsWideEnough() {
        assertEquals(0, BoardCheck.problems(World.fromBoard(Narrow.class)).size());
    }
}
