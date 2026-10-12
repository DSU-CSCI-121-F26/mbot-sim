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

## Pick a room

The `world` in `@MBotLab(world = "open-room")` is the room your robot starts in.

| World | What is in it |
|---|---|
| `"open-room"` | Nothing. Room to draw shapes. |
| `"wall-ahead"` | A wall in front of you. Stop before you hit it. |
| `"parking"` | A parking box made of tape. Find it with the line sensor. |
| `"hallway"` | A hallway that turns right at the end. |

---

## If something goes wrong

| What you see | What to do |
|---|---|
| `package mbot does not exist` | IntelliJ has not loaded the project yet. Right-click `pom.xml` and choose **Maven → Reload project**. |
| No browser tab opened | Open **http://localhost:8121** yourself. |
| The page says *Waiting for your program* | Run `Main` again. Check that **Play new runs** is ticked. |
| The robot did not move but the code ran | Did you make the robot with `new MBot2()` before using it? |
| *"the simulator stopped it"* | A loop never ended. Look at the line with the biggest `×` number. |
