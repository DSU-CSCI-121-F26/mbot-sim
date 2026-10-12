package scout;

import mbot.*;

/** Inheritance: a Scout IS-AN MBot2, plus a scanner it chose to carry. */
public class Scout extends MBot2 {
    private Scanner scanner;

    public Scout() {
        super();                        // build the MBot2 part first
        scanner = new Scanner(this);    // then mount the extra sensor
    }

    /** Drive until something is close, a few cm at a time. */
    public void driveUntilBlocked() {
        while (readUltrasonic() > 25) {
            straight(5);
        }
    }

    /** Look both ways and turn toward the more open side. */
    public void turnTowardOpenSide() {
        double[] d = scanner.scan();
        if (d[6] > d[0]) {
            turnRight(90);
        } else {
            turnLeft(90);
        }
    }

    @Override
    public void run() {
        for (int leg = 1; leg <= 3; leg++) {
            driveUntilBlocked();
            if (leg < 3) {
                turnTowardOpenSide();
            }
        }
    }
}
