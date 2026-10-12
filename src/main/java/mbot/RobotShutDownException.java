package mbot;

/**
 * Thrown when the robot has shut down in a test: it was tagged too many times by drones,
 * or the board's time limit ran out. When you run a program normally, the simulator just
 * ends the run and explains what happened instead.
 */
public class RobotShutDownException extends RuntimeException {

    public RobotShutDownException(String message) {
        super(message);
    }
}
