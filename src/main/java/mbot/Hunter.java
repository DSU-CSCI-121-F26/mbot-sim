package mbot;

/**
 * A drone with four modes, the same idea as a robot state machine:
 *
 * <ul>
 *   <li><b>SEARCH</b>: hover and spin once all the way round, looking for a robot.</li>
 *   <li><b>WANDER</b>: nothing seen, so pick a random spot on the board and fly there.
 *       Then search again.</li>
 *   <li><b>CHASE</b>: fly toward the robot while it can see it.</li>
 *   <li><b>LOST</b>: the robot just disappeared. Keep flying to where it was last seen
 *       for two seconds, then go back to searching.</li>
 * </ul>
 *
 * Whatever mode it is in, the moment it sees a robot it switches to CHASE.
 * Read this class when you write your own drone.
 */
public class Hunter extends Drone {

    private enum Mode { SEARCH, WANDER, CHASE, LOST }

    private Mode mode = Mode.SEARCH;
    private double modeStarted;
    private double targetX, targetY;

    private void switchTo(Mode next) {
        mode = next;
        modeStarted = time();
        if (next == Mode.WANDER) {
            targetX = 20 + random().nextDouble() * (boardWidth() - 40);
            targetY = 20 + random().nextDouble() * (boardHeight() - 40);
        }
    }

    private double timeInMode() {
        return time() - modeStarted;
    }

    @Override
    public void tick() {
        if (seesRobot() && mode != Mode.CHASE) switchTo(Mode.CHASE);
        switch (mode) {
            case SEARCH -> {
                setSpeed(0);
                setTurnRate(60);
                if (timeInMode() >= 6) switchTo(Mode.WANDER);          // one full turn: 360 / 60 = 6 s
            }
            case WANDER -> {
                double off = Patrol.angleTo(Math.toDegrees(Math.atan2(targetY - y(), targetX - x())) - heading());
                setTurnRate(off * 4);
                setSpeed(Math.abs(off) < 30 ? MAX_SPEED : 2);
                boolean arrived = Math.hypot(targetX - x(), targetY - y()) < 10;
                if (arrived || timeInMode() >= 10 || (wallAhead() < 10 && Math.abs(off) < 30)) {
                    switchTo(Mode.SEARCH);
                }
            }
            case CHASE -> {
                setTurnRate(robotAngle() * 3);
                setSpeed(robotDistance() > 25 ? MAX_SPEED : 0);       // close enough: just watch
                if (!seesRobot()) switchTo(Mode.LOST);
            }
            case LOST -> {
                setTurnRate(0);
                setSpeed(wallAhead() > 15 ? MAX_SPEED : 0);
                if (timeInMode() > 2) switchTo(Mode.SEARCH);
            }
        }
    }
}
