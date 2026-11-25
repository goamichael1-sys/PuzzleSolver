# Penguin Solver + Hole Bot (two separate apps, one repo)

## Build with GitHub
1. Create a GitHub repo, upload everything in this folder (keep the .github folder).
2. Actions tab -> "Build APKs" runs automatically.
3. Releases -> "Latest APKs": download **PenguinSolver.apk** (grid puzzle) and **HoleBot.apk** (hole game).

## Use (both apps)
1. Open the app -> "Open Accessibility settings" -> enable it.
2. Turn ON "Show bubble on screen" (or add the app's Quick Settings tile). Long-press the bubble to hide it.
3. Penguin Solver: open a level, tap SOLVE (2 taps per penguin by default; works on any square grid).
4. Hole Bot: open the game, tap START BOT / STOP BOT. If it grabs the wrong spot for the joystick, change "joystick height".
Needs Android 11+.

## Hole Bot notes
- Keeps one finger down on the joystick and re-aims ~3x per second, dragging the hole toward the nearest item.
- Reads the goal bar at the top and only chases item types still needed (hearts, shoes, lipstick/polish).
- Ignores water, the "Lv.N" / "POWER" labels and tutorial bubbles.
