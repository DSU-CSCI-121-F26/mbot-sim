# Build your own board

A **board** is a challenge for a robot: walls, obstacles, a goal, and maybe drones.
You build one in Java, then you (or a classmate) try to get a robot through it.

---

## Step 1: make a board class

Make a new file, `MyMaze.java`, next to `Main.java`. Type this:

```java
import mbot.*;

public class MyMaze extends Board {
    public MyMaze() {
        super("My maze", "Your Name");   // the board's name, and who made it
        map(
            "##########",
            "#S...#..G#",
            "#.##.#.###",
            "#.#..o...#",
            "#...##...#",
            "##########"
        );
    }
}
```

✅ It compiles. A **board** is a class that `extends Board`. You set it up in its constructor.

## Step 2: drive on it

In `Main.java`, change the line above `public class Main` to:

```java
@MBotLab(board = MyMaze.class)
```

Run `Main`.
✅ Your maze shows up in the browser, and your robot starts at the `S`.

---

## The map

Each string is one **row**. Each character is one **square**, 20 cm wide.
The **top** row is the far side of the board. The robot starts at `S`, facing up.

| Character | What it is |
|---|---|
| `#` | Wall |
| `.` or a space | Floor |
| `S` | Start, facing up. Use `>` `<` `v` to face right, left or down instead. |
| `G` | Goal. Its floor is **green**, so a robot with a `ColorSensor` can tell when it has arrived. Squares of `G` that touch become one goal. |
| `o` | A can: round and solid |
| `x` | A box: solid, lower than a wall |
| `-` `\|` `+` | Tape for the line sensor: left to right, up and down, or both |
| `r` `g` `b` `y` | Red, green, blue or yellow floor, for the color sensor |
| `D` | A Sentry drone (see [DRONES.md](DRONES.md)) |

**Make every corridor at least one square wide.** The robot is 17 cm across, and a square is 20 cm.

**If your board has a problem** (the start is inside a wall, or the goal cannot be reached),
the viewer says so in red, and so does IntelliJ's Run panel.

---

## Rules: what counts as passing

Add these in the constructor, after the map:

```java
maxBumps(2);              // more than 2 bumps and the robot fails
timeLimit(60);            // after 60 seconds of robot time, the robot shuts down
requires("ultrasonic");   // which sensors a robot needs for this board
about("Get to the goal without touching the cans.");
```

A robot **passes** the board when it ends with its centre in the goal, stays under the bump
limit, and does not shut down. The viewer shows **✓ Mission complete** or **✗ Mission failed**
at the end.

---

## Placing things exactly

The map is quick. For exact places, use methods instead (or as well). Everything is in cm.
`x` goes to the right and `y` goes up the board. `(0, 0)` is the near-left corner.

```java
size(300, 200);              // the floor, if you do not use map()
start(30, 30, 90);           // x, y, which way it faces (90 = up)
wall(100, 0, 20, 120);       // x, y of the near-left corner, then width, depth
box(200, 80, 30, 30);
can(150, 150);
tape(50, 100, 250, 100);     // from (50, 100) to (250, 100)
floor("red", 240, 160, 40, 30);
goal(250, 150, 40, 40);
```

Loops work too:

```java
for (int i = 0; i < 5; i++) {
    can(60 + i * 40, 120);      // a row of 5 cans, 40 cm apart
}
```

---

## Share your board

Your board is one `.java` file. Send it to a classmate, put it in their project, and set
`@MBotLab(board = YourBoard.class)`. Can their robot beat it?
