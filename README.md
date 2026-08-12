# AutoFish (Fabric mod)

Auto-casts, watches for a bite, reels in, waits a randomized delay, and
re-casts. Open the settings menu with **Right Shift**.

## Why Fabric, and not a universal "injector"

You asked for something that could be injected into *any* client
(Vanilla, Lunar Client, LabyMod, etc.) without a mod loader. That's
technically possible but not a good idea in practice:

- It means writing a Java agent that attaches to the running JVM and
  patches bytecode at runtime — the same technique used by cheat-client
  injectors. It's fragile (breaks on every MC patch), harder for players
  to trust/install safely, and far more likely to get flagged by
  antivirus software than a normal mod jar.
- **Fabric mods already solve your actual goal.** Lunar Client can load
  Fabric mods directly. Vanilla and most other clients just need Fabric
  Loader installed once (a few clicks via the Fabric installer) — after
  that, dropping a `.jar` into the `mods` folder is exactly the same
  "one file, works for everyone" experience you wanted.

If some of your players are on LabyMod and it doesn't load Fabric mods
for them, LabyMod has its own separate add-on API — that would be a
second, LabyMod-specific project, not something this jar can cover.

## Building

You'll need JDK 21 and internet access (Gradle pulls Minecraft,
mappings, and Fabric API automatically).

```bash
./gradlew build
```

The output jar will be in `build/libs/autofish-1.0.0.jar`. Give that
file to your players — they drop it in their `.minecraft/mods` folder
(with Fabric Loader + Fabric API installed for 1.21.11).

**Before building**, double check the version numbers in
`gradle.properties` against https://fabricmc.net/develop/ — Yarn
mappings and Fabric API release constantly, and I can't guarantee the
exact build numbers current at the moment you compile this are still
the latest.

## Notes on the "bite detection"

There's no clean public API for "a fish just bit," so this watches the
fishing bobber's vertical velocity for the same sharp downward jerk a
human player watches for. The threshold in `AutoFishLogic.checkForBite`
(`drop > 0.25 && y < -0.05`) is tuned against vanilla fishing physics —
if your server has custom fishing mechanics (custom loot tables are
fine, but modified bobber physics wouldn't be), you may need to tune
those two numbers.

## Settings (Right Shift menu)

- **On/off toggle**
- **Recast delay min/max (ms)** — randomized wait after reeling in,
  before casting again (defaults to 100–200ms as you asked)
- **Randomized reaction time** — adds a small randomized delay between
  detecting the bite and reeling in, so it doesn't look inhumanly
  instant
- **Pause when inventory full** — stops casting instead of endlessly
  fishing into a full inventory
