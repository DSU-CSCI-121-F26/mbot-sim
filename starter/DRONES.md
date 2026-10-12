# Hide and seek: drones

**Drones** hover over a board looking for your robot. If a drone keeps your robot in sight for
**half a second**, it **tags** the robot. After **3 tags** the robot **shuts down** and the
mission fails. After tagging, a drone needs 2 seconds to recharge before it can tag again.

In the viewer, each drone has a **cone** on the floor: what it can see. The cone is yellow, and
turns **red** when the drone sees your robot. The **Tags left** hearts show how many tags your
robot can still take.

**Drones cannot see through walls or boxes.** Hide behind them.

---

## Put drones on your board

In your board's constructor:

```java
sentry(150, 110);                    // stays in one place and spins
patrol(40, 40, 260, 40, 260, 160);   // flies a loop through these points: x, y, x, y, ...
hunter(150, 200);                    // searches, then chases what it sees
maxTags(3);                          // tags before the robot shuts down (3 if you leave it out)
```

| Drone | What it does |
|---|---|
| **Sentry** | Stays put and spins slowly, like a lighthouse. Move while it looks away. |
| **Patrol** | Flies the same loop over and over. Learn the loop. |
| **Hunter** | Spins to look around, then flies somewhere new. When it sees you, it chases. Lose it behind a wall. |

All drones see 120 cm ahead, 30° to each side, and fly at most 12 cm per second.
Your robot drives 15 cm per second, so you can outrun one.

---

## Know where the drones are: the DroneRadar

Plug a `DroneRadar` into your robot, the same way as any other sensor:

```java
public class Sneak extends MBot2 {
    private DroneRadar radar;

    public Sneak() {
        super("Sneak", "black", "green");
        radar = new DroneRadar(this);
    }
}
```

| Command | What it gives back |
|---|---|
| `radar.watched()` | `true` if any drone can see your robot right now |
| `radar.nearestDistance()` | How far away the closest drone is, in cm |
| `radar.nearestAngle()` | Which way the closest drone is. Positive is to your left. |
| `radar.nearestLookAngle()` | How far the closest drone is looking **away** from you: 0 = right at you, 180 = the other way. Hiding behind a wall? Wait for a big number, then go. |

---

## Write your own drone

A drone is a class that `extends Drone`. You write one method, `tick()`. The simulator calls
`tick()` **10 times a second**. Each time, your drone looks at what it can see and decides what
to do next.

Most drones have **modes**, written with an `enum` and a `switch`. That is a **state machine**:

```java
import mbot.*;

public class Hawk extends Drone {
    private enum Mode { SEARCH, CHASE }
    private Mode mode = Mode.SEARCH;

    @Override
    public void tick() {
        switch (mode) {
            case SEARCH -> {
                setSpeed(0);
                setTurnRate(60);                 // spin and look around
                if (seesRobot()) {
                    mode = Mode.CHASE;
                }
            }
            case CHASE -> {
                setTurnRate(robotAngle() * 3);   // steer toward the robot
                setSpeed(MAX_SPEED);
                if (!seesRobot()) {
                    mode = Mode.SEARCH;
                }
            }
        }
    }
}
```

Put it on a board with `add(new Hawk(), x, y, heading);`.

| Inside `tick()` you can use | What it does |
|---|---|
| `setSpeed(cm)` | Fly forward this many cm per second, up to 12. It keeps going until you change it. |
| `setTurnRate(degrees)` | Turn this many degrees per second. Positive is left. Up to 120. |
| `seesRobot()` | `true` if the robot is in view |
| `robotAngle()` | Which way the robot is from where you face. Positive is left. |
| `robotDistance()` | How far away the robot is, in cm |
| `wallAhead()` | How far you can fly before you hit a wall or box |
| `x()`, `y()`, `heading()` | Where you are and which way you face |
| `boardWidth()`, `boardHeight()` | How big the board is |
| `time()` | Seconds since the robot's program started |
| `random()` | Random numbers that come out the same every run |

**Want a better drone?** Read the `Hunter` class in IntelliJ (hold ⌘ or Ctrl and click on
`Hunter`). It has four modes: SEARCH, WANDER, CHASE and LOST.
