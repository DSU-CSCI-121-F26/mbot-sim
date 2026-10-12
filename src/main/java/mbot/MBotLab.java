package mbot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put this above the class that has your {@code main} method.
 *
 * <pre>
 * &#64;MBotLab(world = "wall-ahead")
 * public class Main {
 *     public static void main(String[] args) {
 *         MBot2 bot = new MBot2();
 *         bot.straight(30);
 *     }
 * }
 * </pre>
 *
 * When you press Run, your robot drives in the 3D viewer in your browser.
 *
 * <p>The {@code world} is the room your robot starts in. The built-in worlds are
 * {@code "open-room"}, {@code "wall-ahead"}, {@code "parking"} and {@code "hallway"}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MBotLab {

    /** Which built-in room the robot starts in. */
    String world() default "open-room";

    /**
     * A board you built: a class that extends {@link Board}. When you set this,
     * {@code world} is ignored. For example: {@code @MBotLab(board = SpiralMaze.class)}.
     */
    Class<? extends Board> board() default Board.class;

    /**
     * The name of a real robot to drive instead of the simulator, such as {@code "StingBot"}.
     * Leave it out to use the simulator. (Driving the real robot comes later in the course.)
     */
    String robot() default "";
}
