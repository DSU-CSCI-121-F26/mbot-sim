# Trace format (format 1)

A trace is one run of a student program, written by `Session` to `~/.mbot-sim/latest.json`
(and a dated copy in `~/.mbot-sim/runs/`, last 50 kept). The viewer reads nothing else, so
anything that writes this format can be replayed — a future hardware run, or a test.

Units: cm, seconds, degrees. Floor coordinates: x right, y away from the viewer.
Heading 0 faces +x, 90 faces +y; positive turns are left (the BRACE `TURN` convention).
Numbers are rounded to 2 decimals.

## Top level

| Key | What |
|---|---|
| `format` | `1` |
| `id` | Changes every save. The viewer polls `/api/latest/id` and reloads when it changes. **Keep it in the first 256 bytes**; the server reads only the head of the file to find it. |
| `created` | Local ISO time |
| `main` | Simple name of the class with `@MBotLab` |
| `worldName`, `world` | The world's name and its full JSON (see WORLDS.md), so a trace replays on its own |
| `robot` | `radius`, `usOffset`, `usHalfAngle`, `usMax`, `lineAhead`, `lineProbes` — for drawing sensors |
| `sources` | `{ "Main.java": "<file text>", ... }` every student file that made a call |
| `frames` | The steps, in order |
| `drones` | Boards with drones: `[{name, kind, mine, pose:[x, y, heading]}]` at the start. Their paths are the `dr` samples in the frames |
| `result` | `{mission, goal?, bumps, maxBumps?, time, timeLimit?, tags?, maxTags?, shutdown?, why:[...], passed}`. A run is a *mission* when the board has a Goal, rules or drones |
| `diagram` | Only when the robot is a student's own class: its class chain, student class first, as `[{name, mine, fields:[{name, type, part?, set?}], methods:[...]}, ...]`. `set:false` = a sensor field still null at the end |

## Frames

One frame per call on `MBot2`, per printed line, plus `start`, `end` and `error`.

| Key | When | What |
|---|---|---|
| `k` | always | `start` `attach` `move` `turn` `drive` `stop` `read` `scan` `led` `print` `note` `shutdown` `end` `error` |
| `f`, `l` | when known | Student file and line that made the call |
| `via` | if called from another student method | `[[file, line], ...]` outward: who called the method that made the call (up to 3) |
| `parts` | `start` | Sensors plugged in at the start: `[]` for a `Chassis`, `["ultrasonic","line"]` for an `MBot2` |
| `part` | `attach` | The sensor just plugged in: `ultrasonic`, `line`, `scanner` or `color` |
| `how` | robot-method calls on a student's subclass | Where the method comes from: "straight() is inherited: Scout gets it from Chassis", or "Inside Scout.straight(), super.straight() runs Chassis's version" |
| `rays` | `scan` | `[[angle, distance, hitX, hitY], ...]`, angle relative to the heading, left positive |
| `color` | color `read` | The floor color returned |
| `radar` | radar `read` | `true`; `v` is the watched flag, a distance or an angle |
| `dr` | boards with drones | Per drone, the samples taken during this step: `[[t, x, y, heading, seeing 0/1], ...]` every 0.1 s |
| `tags` | a tag landed | `[[t, droneIndex], ...]` |
| `problems` | `start` | Board problems found before the run (BoardCheck) |
| `result` | `end` | Same as the top-level `result` |
| `c` | always | The call as text, e.g. `straight(30)` |
| `t0`, `t1` | always | Robot time at the start and end of the step |
| `p` | if it moved | Poses `[[x, y, heading], ...]` passed through. Headings are unwrapped within a frame, so a 360° spin is 90 → 450 |
| `pose` | always but `error` | Pose at the end of the step |
| `m` | always | The plain-English narration shown in "What's happening" |
| `bump` | if it hit a wall | `true` |
| `v` | `read` | The value returned to the student's code |
| `hit` | ultrasonic `read` | `[x, y]` where the echo came from |
| `line` | line-sensor `read` | `true` (tells the two kinds of read apart) |
| `led` | `led` | `[id, r, g, b]`; all zero = off |
| `out` | `print` | The printed line |

## Time

The robot moves in 0.01 s steps, and everything else moves with it: each step advances the
clock, flies every drone, checks what each drone can see, and lands tags. Drones' `tick()` runs
every 0.1 s of robot time. A drone tags after 0.5 s of continuous sight, then recharges for 2 s.
At the board's `maxTags` (default 3), or past its `timeLimit`, the robot shuts down: in a normal
run the simulator saves and exits with a message; in a test, the next command throws
`RobotShutDownException` (`Testing.run` catches it).

Blocking calls (`straight`, `turn*`, timed `forward`) advance robot time by their duration.
While the wheels run freely (`forward(speed)`, `setMotorPower`), the gap between two calls is
`max(0.02 s, wall-clock time)` — a sensor read is never free on the real robot either, and a
`Thread.sleep` really lets the robot drive. Motion during that gap is folded into the next
call's frame. Idle gaps over 0.2 s are kept so the replay pauses where the program paused.
