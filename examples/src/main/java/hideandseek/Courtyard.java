package hideandseek;

import mbot.*;

/** A student-style board: a map, cover to hide behind, a goal, and two drones. */
public class Courtyard extends Board {
    public Courtyard() {
        super("Courtyard", "Ms. Example");
        map(
            "###############",
            "#..........GGG#",
            "#..........GGG#",
            "#..######.#####",
            "#.............#",
            "#.............#",
            "#.............#",
            "#..x....x.....#",
            "#S............#",
            "###############"
        );
        sentry(150, 110);
        patrol(260, 40, 260, 140);
        maxTags(3);
        about("Sneak up the left side and along the top to the goal. The sentry spins; the patrol paces.");
    }
}
