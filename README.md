# Penguin Solver
Accessibility-service app that reads the penguin puzzle board from a screenshot, solves it, and taps the cells.

## Build with GitHub
1. Create a new GitHub repo, upload everything in this folder (keep the .github folder).
2. Open the Actions tab -> "Build APK" runs automatically (or Run workflow).
3. When green, open Releases -> "Penguin Solver (latest)" -> download app-debug.apk.

## Use
1. Install APK, open it, tap "Open Accessibility settings", enable Penguin Solver.
2. Turn ON "Show SOLVE bubble" in the app (or use the Quick Settings tile). It is OFF by default. Open the game on a level, tap the blue SOLVE bubble (drag it; long-press hides it).
3. Default is 2 taps per penguin cell (X then penguin). Change "Taps per cell" in the app if needed.
Works on any square grid size (tested 4x4 to 12x12). Needs Android 11+.
