import mbot.*;

@MBotLab(world = "open-room")
public class Main {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();

        // Drive a square: 4 sides, 4 corners.
        for (int side = 1; side <= 4; side++) {
            bot.straight(40);
            bot.turnLeft(90);
            System.out.println("Finished side " + side);
        }
    }
}
