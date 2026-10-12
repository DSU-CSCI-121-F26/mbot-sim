package mbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MBot2's two constructors: the default look, and a name and colors of your own. */
class IdentityTest {

    @Test
    void theDefaultConstructorGivesTheStandardLook() {
        Testing.start("open-room");
        MBot2 bot = new MBot2();
        assertEquals("MBot2", bot.getName());
        assertEquals("blue", bot.getBodyColor());
        assertEquals("sky", bot.getEyeColor());
        assertEquals("MBot2", bot.toString());
    }

    @Test
    void aNameAndColorsOfYourOwn() {
        Testing.start("open-room");
        MBot2 bot = new MBot2("Sparky", "red", "#ffcc00");
        assertEquals("Sparky", bot.getName());
        assertEquals("red", bot.getBodyColor());
        assertEquals("#ffcc00", bot.getEyeColor());
        assertEquals("Sparky", "" + bot);
    }

    @Test
    void anUnknownColorIsExplained() {
        Testing.start("open-room");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new MBot2("Sparky", "plaid", "red"));
        assertTrue(e.getMessage().startsWith("\"plaid\" is not a color the robot knows (for bodyColor)"), e.getMessage());
    }

    @Test
    void aRobotNeedsAName() {
        Testing.start("open-room");
        assertThrows(IllegalArgumentException.class, () -> new MBot2("  ", "red", "red"));
    }

    static class Scout extends MBot2 {
        Scout() {
            super("Scout", "orange", "white");
        }
    }

    @Test
    void aSubclassPassesItsLookUpWithSuper() {
        Testing.start("open-room");
        MBot2 bot = new Scout();
        assertEquals("Scout", bot.getName());
        assertEquals("orange", bot.getBodyColor());
    }
}
