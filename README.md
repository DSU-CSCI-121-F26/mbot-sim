# mbot-sim

**A 3D simulator for the mBot2, for CSCI-121.** Students write plain Java against an `MBot2`
object, press Run in IntelliJ, and watch the robot drive in their browser, with their own code
beside it and the running line highlighted.

```java
import mbot.*;

@MBotLab(world = "wall-ahead")
public class Main {
    public static void main(String[] args) {
        MBot2 bot = new MBot2();
        bot.forward(50);
        while (bot.readUltrasonic() > 20) {
            // keep driving
        }
        bot.stop();
    }
}
```

From Part 21 students build the robot from parts instead (`Chassis` + `Ultrasonic` +
`LineSensor`) inside their own class, which is how the course teaches composition. From
Part 23 they write their own robot class that `extends MBot2`, add the extra sensors it needs
(`Scanner`, `ColorSensor`) as fields, and override `run()`, which is how it teaches inheritance.
The viewer draws their class chain as a diagram.

That file is everything a student touches: one import, one annotation, `main`, and their code.
No setup call, no save call, no server, no HTML, no JavaScript. The method names match the
BRACE `MBot2` client, so the same code is meant to drive the real robot later
(see [docs/ROADMAP.md](docs/ROADMAP.md)).

---

## How it works

It is modelled on Spring Boot: an annotation on the main class, and auto-configuration
behind the first object the student makes.

```
 student's Main.java                       mbot-sim jar (the student never opens it)
 ───────────────────                       ──────────────────────────────────────────
 @MBotLab(world = ...)          ──reads──▶ Session  (started by the first new MBot2())
 MBot2 bot = new MBot2();                   ├─ World  ← worlds/*.json
 bot.straight(30);              ──each──▶   ├─ Sim    physics: drive, bump, ultrasonic, line sensor
 System.out.println(...)        ──tee───▶   ├─ Sources  which file + line made each call
                                            └─ on exit (shutdown hook):
                                                 ~/.mbot-sim/latest.json   ◀── the trace
                                                 Viewer: is the server up? if not, start it
                                                         in a background JVM; open a tab
                                                         if none is watching
 browser tab  ◀── polls /api/latest/id every second ── ViewerMain (HTTP on 127.0.0.1:8121)
               ◀── /, /vendor/three.*.js (bundled, works offline), /api/latest
```

- **The trace is the only contract** between the Java side and the page
  ([docs/TRACE_FORMAT.md](docs/TRACE_FORMAT.md)), the same split as the CSCI 558 farm-drone
  viewer. Anything that writes a trace can be replayed, including a future real-robot run.
- **Every frame knows its line.** `StackWalker` finds the innermost student frame for each call,
  and the source files are copied into the trace, so the page can show the code next to the
  robot and say "this line has run 289 times".
- **Nothing lands in the student's repo.** Runs go to `~/.mbot-sim/` (last 50 kept). The
  viewer server is a separate JVM started from the same classpath, so it outlives the
  program and is always the same version as the library. It quits after 30 minutes with no
  tab open, and only listens on localhost.
- **Runaway loops are stopped** after 10 minutes of robot time or 20,000 calls, with a
  message pointing at the loop. A crash is recorded on the student's line and still printed
  to IntelliJ's console as usual.
- **No runtime dependencies.** One jar, Java 17+, works with no internet.

## Layout

| Path | What |
|---|---|
| `src/main/java/mbot/` | What students use: `MBotLab`, `MBot2`, `Chassis`, `Ultrasonic`, `LineSensor`, `Scanner`, `ColorSensor`; and `Testing` for lab tests |
| `src/main/java/mbot/internal/` | Session, Sim, World, Sources, Tee, Viewer, ViewerMain, Specs, Json |
| `src/main/resources/mbot/viewer/` | `index.html` and three.js r178 (`vendor/`, MIT) |
| `src/main/resources/mbot/worlds/` | Built-in worlds ([docs/WORLDS.md](docs/WORLDS.md)) |
| `src/test/java/mbot/` | Physics and JSON tests, with numbers worked out by hand |
| `starter/` | The student template: `pom.xml`, `Main.java`, and a student README |
| `examples/` | Classroom demos (not lab answers) |

## Build and try it

```bash
mvn install                       # builds, runs the tests, installs edu.desu.cis:mbot-sim locally
cd starter && mvn compile         # then open starter/ in IntelliJ and run Main
```

Handy system properties (for the instructor, never needed by students):

| Property | Effect |
|---|---|
| `-Dmbot.viewer=off` | Save the run but do not start or open the viewer (CI, tests) |
| `-Dmbot.open=off` | Start the viewer if needed, but do not open a browser tab |
| `-Dmbot.port=8799` | Use another port |
| `-Dmbot.home=/some/dir` | Save runs somewhere other than `~/.mbot-sim` |

`http://localhost:8121/#step=300&cam=top` opens a run paused at step 300 from the top
camera, which is useful for pointing a student at one moment.

## Getting it to students

Releases are published to a Maven repo hosted on GitHub Pages:
**https://dsu-csci-121-f26.github.io/mbot-sim/maven**. No login, nothing builds on request: it is
plain files on GitHub's servers. A lab's `pom.xml` needs these two blocks (see `starter/pom.xml`):

```xml
<repositories>
  <repository>
    <id>mbot-sim</id>
    <url>https://dsu-csci-121-f26.github.io/mbot-sim/maven</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>edu.desu.cis</groupId>
    <artifactId>mbot-sim</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

### Releasing a new version

1. Change `<version>` in `pom.xml` (e.g. `0.1.1` for a fix, `0.2.0` for new features) and commit.
2. `git tag v0.1.1 && git push origin main v0.1.1`
3. The **Release** workflow builds and tests the tag, then adds the jar to the `gh-pages`
   branch. It refuses if the tag and the pom disagree, or if that version was already published.
4. Update the `<version>` in the lab templates that should get it.

**Never re-publish a version.** Labs pin an exact version so a student's project cannot change
under them in the middle of an assignment. Every change is a new number. No `-SNAPSHOT`s for
students.

Lab answers must never be committed here: this repo is public.

## Relationship to StingSim

StingSim (`../stingsim`) pretends to be an mBot2 **over the network protocol**, for the term
project's `RobotController` programs. mbot-sim is the **beginner front door**: no networking,
no `RobotController`, just `new MBot2()`. They meet at v1.0: mbot-sim's hardware backend will
speak the same protocol, and can be tested against StingSim before anyone touches a robot.
