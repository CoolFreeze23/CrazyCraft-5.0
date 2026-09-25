# Server pack internals

The server pack is the client pack minus the client-only mods, plus scripts that install NeoForge and start the server. Every release is boot-tested on a headless server before it goes out.

## Layout

```
CrazyCraft5-Server/
  start.bat / start.sh            run this
  neoforge-21.1.248-installer.jar
  server.properties               defaults explained below
  mods/                           163 jars
  config/                         same as the client
  defaultconfigs/
  mcheli/                         MCHeli data
  moonlight-global-datapacks/
  REMOVED_CLIENT_MODS.txt         exact list of what was left out
  README.txt
```

## What the start script does

1. Checks that Java 21 is installed (and tells you where to get it if not).
2. On the first run only, runs the bundled NeoForge installer (`--install-server`), which downloads the vanilla server jar and libraries from Mojang and NeoForged.
3. Asks you to accept the Minecraft EULA (writes `eula.txt` when you answer `yes`).
4. Starts the server with G1GC and the RAM set at the top of the script (default `6G`).

The script is about 50 lines, so it's easy to read and change.

## Removed client-only mods

Of the pack's 202 mods, 39 are client-side only (rendering, HUD, menus, audio, cosmetics) and are left out of the server, which runs the other 163. The ones removed are the Sodium/Iris graphics stack, FancyMenu and its libraries, animation and particle mods, AmbientSounds, camera and HUD tools, and the Fabric cosmetic mods. The exact list ships in the pack as `REMOVED_CLIENT_MODS.txt`.

Players connect with the normal client pack. The server accepts them because every gameplay mod is the same on both sides.

## Shipped server.properties defaults

| Setting | Value | Why |
|---|---|---|
| `allow-flight` | `true` | Several mods let players fly; this stops false "kicked for flying" kicks |
| `view-distance` / `simulation-distance` | `8` | Reasonable for a 163-mod server. Raise it if your hardware allows |
| `max-tick-time` | `-1` | Turns off the watchdog. Heavy modded worldgen can go past the vanilla 60 s limit, and the watchdog would then kill the server for no real reason |
| `spawn-protection` | `0` | Modpack players expect to build at spawn |
| `enable-command-block` | `true` | Some structures use command blocks |

## Performance notes

- The first boot generates the world and is the slowest. Several minutes is normal.
- Lithium, FerriteCore, ModernFix and Clumps run on the server side and are kept.
- Chunk Pregenerator is included. Pregenerating around spawn (`/pregen start gen radius ...`) makes early play a lot smoother.
- 6 GB of RAM works and 8 GB is comfortable. Going past 10 GB doesn't help much.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `You need to agree to the EULA` loop | Delete `eula.txt`, run the start script again and answer `yes` |
| Install step fails | Almost always a flaky internet connection. Run the script again |
| Crash mentioning a mod from `REMOVED_CLIENT_MODS.txt` | You copied client mods in by hand. Restore `mods/` from the zip |
| Long "remapping" pause on first boot | Normal. Sinytra Connector is converting the Fabric mods, and the result is cached after that |
