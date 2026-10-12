# Roadmap: from the simulator to the real mBot2

**The goal for the semester:** by the end of the course, students drive their own mBot2 with
Java. The simulator is how they get there. Every method on `MBot2` has the same name, units
and meaning as the BRACE client's `MBot2` (`labs/week07-brace-square/.../commands/MBot2.java`),
so the switch to hardware is one setting in the annotation, not a rewrite:

```java
@MBotLab(world = "wall-ahead")      // now: the simulator
@MBotLab(robot = "StingBot")        // later: the student's own robot, same code
```

## v0.1 — loops and if (Follow-Along Parts 06–09) · built

- `MBot2`: `straight`, `turnLeft/Right`, `forward` (open and timed), `setMotorPower`, `stop`,
  `readUltrasonic`, `readLineStatus`, `turnLedOn/Off`.
- Worlds: `open-room`, `wall-ahead`, `parking`, `hallway`.
- Viewer: replay with play/step/scrub, three cameras, the student's code with the running line
  highlighted and run counts, plain-English narration, printed output, sensor beam and probes,
  bumps, crashes on the right line, runaway-loop stop.

## v0.2 — arrays and methods (Parts 10–14)

- Worlds with **checkpoints** (an array of zones to visit in order). The viewer ticks each one
  off; the end card says which were missed.
- A `goal` flag on zones and a pass/fail end card ("Parked inside P ✓").
- Headless runs for labs: `Session.headless(world)` already exists. Add a tiny JUnit helper so a
  lab's given test can assert "ended inside the goal without bumping", run in CI.

## v0.3 — objects (Parts 15–20)

- Students write their own classes around `MBot2` (a `Driver`, a `Patrol`). The viewer already
  shows every file that made a call, as tabs, and jumps to the file that is running.
- Optional: draw the student's own fields (e.g. a `mode` String) in the HUD — needs a small,
  explicit API so it is not magic. Decide with the class plan.

## v0.4 — state machines (week of the FSM lecture)

- A `mode` readout and a timeline strip coloured by mode.
- Ties to the existing Robocode `switch (mode)` upgrade and to the term project's `MazeRobot`.

## v1.0 — the real robot

`@MBotLab(robot = "...")` switches `Session` from the simulator to a **BRACE backend** that
speaks `stingsim/docs/PROTOCOL.md`: UDP discovery on 9998, TCP commands on 9990, telemetry on
9991. Same student code, same viewer: the trace is recorded from the real robot's replies
(sensor values are real; the pose is estimated from the commands).

- Test the backend against **StingSim** first. StingSim already pretends to be an mBot2 over
  that protocol, so the hardware path can be proven with no robot on the desk.
- Carry over the protocol traps PROTOCOL.md records: the request uses `payload.robot` but the
  reply uses `payload.robotId`; discovery skips loopback, so a laptop with no network interface
  up never finds the robot; never pretty-print a command line.
- Add an IP override (`@MBotLab(robot = "StingBot", ip = "...")`) so a classroom network that
  blocks broadcast still works.

## Calibrate against a real robot (needs someone with an mBot2, a tape measure and a stopwatch)

Every number in `Specs.java` marked UNCALIBRATED, plus three behaviours to confirm:

| Check | Why |
|---|---|
| `straight()` and `turnLeft()` speeds | Replay timing; and how far `forward(50)` goes per second |
| `forward(speed)` units | The sim treats speed as wheel RPM (mBot2 encoder motors, max ~200) |
| `readLineStatus()` bit order | The sim uses far-left = 8 … far-right = 1. Confirm on the quad RGB sensor |
| `setMotorPower` signs | The sim assumes positive = forward for both wheels |
| Ultrasonic range and blind zone | The sim reads 3–300 cm with a ±15° cone |

## Open decisions

1. **Distribution.** Decided: a Maven repo on GitHub Pages (`CD-CSCI-10447/mbot-sim`, see
   README). Maven Central later would remove the `<repository>` block from every lab; the
   code would not change.
2. **Where lab answers live.** Not in this repo (it is public). Use the course's
   gitignored `solution/` convention, or a private instructor kit like CSCI 558's.
3. **Who builds v1.0.** The BRACE backend is about a week of focused work plus a session with a
   real robot.
