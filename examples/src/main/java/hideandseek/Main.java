package hideandseek;

import mbot.*;

@MBotLab(board = Courtyard.class)
public class Main {
    public static void main(String[] args) {
        MBot2 bot = new Sneak();
        bot.run();
    }
}
