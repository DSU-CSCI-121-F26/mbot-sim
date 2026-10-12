# mBot2 Starter

Write Java for your mBot2 robot, then watch it drive in 3D in your browser.

The code you write here is the same code that will drive your real mBot2 later in the course.

---

## Run it (2 minutes)

1. Open this folder in IntelliJ.
   ✅ You see `src/main/java/Main.java` in the Project panel on the left.
2. Open `Main.java`.
3. Click the green ▶ next to `public static void main` and choose **Run 'Main.main()'**.
   ✅ A browser tab opens. Your robot drives a square.
   ✅ The **Run** panel at the bottom of IntelliJ prints `Finished side 1` … `Finished side 4`.
4. Change `40` to `80` in `bot.straight(40);` and run it again.
   ✅ The tab you already have open replays the new run by itself. The square is bigger.

The first run can take a few seconds while the viewer starts. After that it is quick.

---

## What you are looking at

| Part of the page | What it shows |
|---|---|
| The 3D room | Your robot, the walls, and a blue line for everywhere it has driven. |
| **What's happening** | The line of your code that is running right now, in plain English. |
| **Your code** | Your file. The yellow line is the one running. `×4` means that line has run 4 times. |
| **Printed output** | Anything you printed with `System.out.println`. |
| The buttons at the bottom | ▶ plays. ◀ and ▶\| go one step at a time. Drag the slider to any step. |

**Overview**, **Follow** and **Top** change the camera. In Overview and Top you can drag to look around and scroll to zoom.

---

## Name your robot

`new MBot2()` makes the standard robot: named "MBot2", blue, with sky-blue eyes.
You can also give it a name and colors:

```java
MBot2 bot = new MBot2("Sparky", "red", "yellow");   // name, body color, eye color
```

Colors: `red`, `orange`, `yellow`, `green`, `blue`, `sky`, `purple`, `pink`, `teal`, `white`,
`black`, `gray`, `gold`, or a hex code like `"#ff8800"`. The name shows on the robot's screen.
`bot.getName()` gives it back, and `System.out.println(bot)` prints it.

---

## The robot's commands

Every command starts with `bot.` (or whatever you named your robot).

| Command | What it does |
|---|---|
| `bot.straight(30);` | Drive forward 30 cm, then stop. A negative number drives backward. |
| `bot.turnLeft(90);` | Turn left 90 degrees on the spot. |
| `bot.turnRight(90);` | Turn right 90 degrees on the spot. |
| `bot.forward(50);` | Start driving at speed 50 and **keep going**. Your next line runs right away. |
| `bot.forward(50, 2);` | Drive at speed 50 for 2 seconds, then stop. |
| `bot.setMotorPower(30, 60);` | Set each wheel's power, from -100 to 100. Different powers make it curve. |
| `bot.stop();` | Stop both wheels. |
| `bot.readUltrasonic()` | How far away the nearest thing in front is, in cm. Nothing in range gives 300. |
| `bot.readLineStatus()` | What the line sensor under the robot sees, from 0 to 15. 0 means no tape. |
| `bot.turnLedOn(1, 255, 0, 0);` | Turn light 1 red. Lights are 1 to 5. Colors are red, green, blue from 0 to 255. |
| `bot.turnLedOff(1);` | Turn light 1 off. |

---

## Build your own robot from parts (Part 21 and later)

`MBot2` is the whole robot, already put together. You can also build one from parts.
A **`Chassis`** is just the wheels, motors and CyberPi. It has **no sensors** until you plug them in:

```java
Chassis base = new Chassis();               // make the robot first
Ultrasonic eyes = new Ultrasonic(base);     // then plug sensors into it
LineSensor floor = new LineSensor(base);
```

| Command | What it does |
|---|---|
| `eyes.distance()` | How far away the nearest thing in front is, in cm. Nothing in range gives 300. |
| `floor.status()` | What the line sensor sees, from 0 to 15. 0 means no tape. |
| `floor.seesTape()` | `true` if any of the four probes is over tape. |

A `Chassis` drives with the same commands as `MBot2`: `straight`, `turnLeft`, `turnRight`, `forward`, `stop`, and the lights.

In the 3D view, your robot only shows the sensors you plugged in.

---

## Make your own robot class (Part 23 and later)

Write a class that **extends** `MBot2`. Your robot can do everything an `MBot2` can, and you
add the extra sensors it needs as **fields**:

```java
public class Scout extends MBot2 {
    private Scanner scanner;            // an extra sensor this robot needs

    public Scout() {
        super("Scout", "orange", "white");   // build the MBot2 part first, with a name and colors
        scanner = new Scanner(this);    // then mount the extra sensor
    }

    @Override
    public void run() {                 // your robot's mission
        straight(30);
    }
}
```

In `main`: `MBot2 bot = new Scout(); bot.run();`

| Extra sensor | Command | What it does |
|---|---|---|
| `Scanner` | `scanner.scan()` | Looks 7 ways and gives back a `double[]` of distances, far left (index 0) to far right (index 6). Index 3 is straight ahead. |
| | `scanner.look(45)` | Looks one way: positive is left, negative is right. |
| `ColorSensor` | `eye.color()` | The floor color: `"white"`, `"black"`, `"red"`, `"green"`, `"blue"` or `"yellow"`. |

The **Your robot** box in the viewer draws your class and the classes it extends. A sensor
field you forgot to plug in shows up in red as `= null`.

---

## Pick a room

The `world` in `@MBotLab(world = "open-room")` is the room your robot starts in.

| World | What is in it |
|---|---|
| `"open-room"` | Nothing. Room to draw shapes. |
| `"wall-ahead"` | A wall in front of you. Stop before you hit it. |
| `"parking"` | A parking box made of tape. Find it with the line sensor. |
| `"hallway"` | A hallway that turns right at the end. |
| `"obstacles"` | Boxes and cans between you and the goal. Needs an `Ultrasonic`. |
| `"line-track"` | A loop of tape to follow. Needs a `LineSensor`. |
| `"color-zones"` | Colored stripes, then a red stop pad. Needs a `ColorSensor`. |
| `"junctions"` | A corridor that turns right, then left. Needs a `Scanner`. |

---

## Build your own boards, and play hide and seek

- **[BOARDS.md](BOARDS.md):** draw your own board with text, add a goal and rules, and share it.
- **[DRONES.md](DRONES.md):** put drones on a board that hunt your robot, use a `DroneRadar` to
  sneak past them, and write your own drone.

---

## If something goes wrong

| What you see | What to do |
|---|---|
| `package mbot does not exist` | IntelliJ has not loaded the project yet. Right-click `pom.xml` and choose **Maven → Reload project**. |
| No browser tab opened | Open **http://localhost:8121** yourself. |
| The page says *Waiting for your program* | Run `Main` again. Check that **Play new runs** is ticked. |
| The robot did not move but the code ran | Did you make the robot with `new MBot2()` before using it? |
| *"eyes is null"* | You declared a sensor but never made it. Add `eyes = new Ultrasonic(base);` to the constructor. |
| *"needs a robot to plug into"* | You made a sensor before the robot. Make the `Chassis` first. |
| *"the simulator stopped it"* | A loop never ended. Look at the line with the biggest `×` number. |
