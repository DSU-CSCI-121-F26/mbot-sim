# Worlds

A world is a JSON file. Built-ins live in `src/main/resources/mbot/worlds/`; a student or
instructor can also point at a file: `@MBotLab(world = "worlds/maze1.json")` (relative to the
project folder).

```json
{
  "title": "Parking spot",
  "about": "One sentence for the viewer.",
  "size": [240, 300],
  "start": {"x": 120, "y": 40, "heading": 90},
  "walls": [ {"x": 60, "y": 270, "w": 120, "h": 10} ],
  "tape":  [ {"points": [[90,170],[150,170],[150,230],[90,230]], "closed": true, "width": 2.5} ],
  "zones": [ {"x": 90, "y": 170, "w": 60, "h": 60, "label": "P"} ],
  "objects": [ {"shape": "box", "x": 115, "y": 90, "w": 70, "h": 20, "color": "#c8823a"},
               {"shape": "can", "x": 70, "y": 165, "r": 6, "color": "#d6453d"} ],
  "requires": ["line"]
}
```

| Key | Meaning |
|---|---|
| `size` | Floor `[width, depth]` in cm. A wall is added around the edge automatically. |
| `start` | Robot centre and heading (90 = facing away from the viewer) |
| `walls` | Solid boxes, `x`/`y` = the corner nearest the origin. The robot bumps them and the ultrasonic sees them |
| `tape` | Dark tape on the floor, as a polyline. Only the line sensor sees it |
| `zones` | Coloured patches with a label. Nothing reacts to them while driving; tests ask `Testing.insideZone("P")` |
| `objects` | Things to detect: `box` (solid, like a short wall) or `can` (round, radius `r`). The robot bumps them and the ultrasonic sees them |
| `patches` | Colored floor for the `ColorSensor`: `{"x","y","w","h","color"}`, color one of `red` `green` `blue` `yellow`. Later patches sit on top; tape reads `black`, bare floor `white` |
| `requires` | Sensors the world is built for (`"ultrasonic"`, `"line"`, `"scanner"`, `"color"`). If the run ends without one plugged in, the viewer says so |

Built-in worlds: `open-room`, `wall-ahead`, `parking`, `hallway`, `obstacles`, `line-track`, `color-zones`, `junctions`.

**Graded lab challenges should use their own world file in the lab repo**, not a built-in one:
this repo is public, and its tests contain routes that solve the built-in worlds.

Keep numbers that are easy to check by hand: tests in `src/test/java/mbot/SimTest.java` assert
distances worked out from these files (e.g. wall-ahead: sensor at y 47, wall face at 200 → 153 cm).
