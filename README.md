# Sprinkle Client 🍩

A **Donut SMP** themed Minecraft client for **1.21.11** (Fabric). By **DarkerDawnFR**.
Every button says **Donut SMP**, and every one of them takes you to Donut SMP.

> Fan-made. Not affiliated with DrDonut or Donut SMP.

## Install
- **Windows:** download **SprinkleClient-Installer.exe** from Releases and press **INSTALL**.
- **Mac / Linux:** download **SprinkleClient-Installer.jar** and double-click it (needs Java 17+).

Then open the Minecraft Launcher, pick the **Sprinkle Client** profile and press **Play**.
The installer adds Fabric for 1.21.11, Sprinkle Client and the speed mods (Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, ModernFix, Iris, Zoomify, Mod Menu). Untick any of them under **Options**.

## The main menu
- Donut SMP logo (drop your own `logo.png` in `config/sprinkleclient/` to replace it).
- Every button says **Donut SMP** and joins `donutsmp.net`. **Quit** is now **Rage Quit** (with a sad trombone).
- Bottom-left icon: **Credits**. Bottom-right of the button row: **Discord** (opens discord.gg/donutsmp).
- Splash texts are Donut SMP jokes. Background is a slideshow of your pictures from `config/sprinkleclient/backgrounds/` (or your screenshots).
- Click the logo 10 times. 🍩
- **Settings gear (bottom right):** switch back to the vanilla menu, choose the join style (funny loading screen / instant / "are you sure?" popup), sounds, links and folders.
- Ka-ching button clicks and original menu music.

## Donut mods (Right Shift)
Each has an on/off switch and its own keybind (Options → Controls → Sprinkle Client).

| Mod | What it does |
|---|---|
| Money HUD | Your balance from the sidebar + how much you made today |
| Fake Scoreboard | Your own Donut-style sidebar with any name/money/stats. New or Old style. Always says **"These are not real stats"**. Off = the real one comes back |
| Streamer Mode | Hides coordinates in F3, chat and the HUD |
| PvP HUD | Combat-tag timer, totem pop counter, armour durability |
| Death Saver | Saves where you died, arrow back to it, a joke on the death screen |
| Spawner Calculator | Estimated spawner value on tooltips (your own prices in the config) |
| Base Alarm | Pings when a player is near your base. **Off by default**: Donut SMP may treat it as a radar |
| Rich Flex | Big celebration when you hit $1M, $1B... |
| Sky Changer | Put a .png/.jpg in `config/sprinkleclient/sky/` and it becomes the sky |

## Resource packs (on by default)
- **Donut SMP** – cursed: pigs are frosted donut-pigs that meow, cows and chickens meow too, sheep go ka-ching, the sun and moon are donuts.
- **Sprinkle Blue Buttons** – blue frosting buttons everywhere.

## Build
- `mod/` – Fabric mod (`gradle build`).
- `installer-jar/` – Swing installer (`gradle -p installer-jar build -PmodJar=...`).
- `installer-exe/` – Go + WebView2 Windows installer.
- `tools/gen_art.py`, `tools/gen_sounds.py` – rebuild all art and sounds (all sounds are synthesized, no samples).
- GitHub Actions builds everything, runs the installers against real Fabric/Modrinth downloads, and launches the game for an in-game test with screenshots. Run **Release** to publish.
