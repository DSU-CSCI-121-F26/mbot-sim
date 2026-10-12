package mbot;

import mbot.internal.Session;

/**
 * Run a mission on a board chosen while the program runs, instead of with {@code @MBotLab}.
 * The run is recorded and replayed in the viewer, the same as a normal run.
 *
 * <pre>
 * Launch.on(new SpiralMaze(), () -&gt; new Scout().run());
 * </pre>
 *
 * Tools like the arena use this to replay one match. Most programs do not need it.
 */
public final class Launch {

    private Launch() {}

    /**
     * @param board the board to drive on
     * @param mission what to do: make the robot and run it
     */
    public static void on(Board board, Runnable mission) {
        Session.startLive(board, "");          // no main class to show: the viewer leaves it out
        mission.run();
    }
}
