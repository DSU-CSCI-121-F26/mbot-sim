package scout;

import mbot.*;

@MBotLab(world = "junctions")
public class Main {
    public static void main(String[] args) {
        MBot2 bot = new Scout();    // an MBot2 variable holding a Scout
        bot.run();                  // runs Scout's run(), not MBot2's
    }
}
