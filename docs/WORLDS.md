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
  "zones": [ {"x": 90, "y": 170, "w": 60, "h": 60, "label": "P"} ]
}
```

| Key | Meaning |
|---|---|
| `size` | Floor `[width, depth]` in cm. A wall is added around the edge automatically. |
| `start` | Robot centre and heading (90 = facing away from the viewer) |
| `walls` | Solid boxes, `x`/`y` = the corner nearest the origin. The robot bumps them and the ultrasonic sees them |
| `tape` | Dark tape on the floor, as a polyline. Only the line sensor sees it |
| `zones` | Coloured patches with a label. Drawing only — nothing in the sim reacts to them yet |

Keep numbers that are easy to check by hand: tests in `src/test/java/mbot/SimTest.java` assert
distances worked out from these files (e.g. wall-ahead: sensor at y 47, wall face at 200 → 153 cm).
