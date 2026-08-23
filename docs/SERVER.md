# Server pack internals

The server pack is the client pack minus client-only mods, plus NeoForge install/start automation. It is boot-tested headless before every release.

## Layout

```
CrazyCraft5-Server/
├── start.bat / start.sh       ← run this
├── neoforge-21.1.248-installer.jar
├── server.properties           (sane defaults, see below)
├── mods/                       (162 jars)
├── config/                     (identical to client)
├── defaultconfigs/
├── mcheli/                     (MCHeli data)
├── moonlight-global-datapacks/
├── REMOVED_CLIENT_MODS.txt     (exact list of what was filtered out)
└── README.txt
```

## What the start script does

1. Verifies Java 21 is installed (tells you where to get it if not).
2. First run only: executes the bundled NeoForge installer (`--install-server`), which downloads the vanilla server jar and libraries from Mojang/NeoForged.
3. Prompts you to accept the Minecraft EULA (writes `eula.txt` on `yes`).
4. Starts the server with G1GC and the RAM set at the top of the script (default `6G`).

Nothing is hidden — the script is ~50 lines, readable, and editable.

## Removed client-only mods

36 mods are client-side only (rendering, HUD, menus, audio, cosmetics) and are excluded from the server: the Sodium/Iris graphics stack, FancyMenu and its libraries, animation and particle mods, AmbientSounds, camera/HUD tools, and the Fabric cosmetic mods. The full exact list ships in the pack as `REMOVED_CLIENT_MODS.txt`.

Clients connect with the normal client pack — the server accepts them because every gameplay mod is identical on both sides.

## Shipped server.properties defaults

| Setting | Value | Why |
|---|---|---|
| `allow-flight` | `true` | Several mods grant flight; prevents false "kicked for flying" |
| `view-distance` / `simulation-distance` | `8` | Sensible for a 156-mod server; raise if your hardware allows |
| `max-tick-time` | `-1` | Disables the watchdog — heavy modded worldgen can exceed the vanilla 60s limit and would otherwise kill the server spuriously |
| `spawn-protection` | `0` | Modpack players expect to build at spawn |
| `enable-command-block` | `true` | Used by some structures |

## Performance notes

- First boot generates the world and is the slowest — several minutes is normal.
- Lithium, FerriteCore, ModernFix, and Clumps run server-side and are kept.
- Chunk Pregenerator is included: pregenerating spawn (`/pregen start gen radius ...`) massively smooths early play.
- 6 GB RAM works; 8 GB is comfortable. Beyond 10 GB has diminishing returns.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `You need to agree to the EULA` loop | Delete `eula.txt` and run the start script again, answer `yes` |
| Install step fails | Almost always internet flakiness — run the script again |
| Crash mentioning a mod from `REMOVED_CLIENT_MODS.txt` | You copied client mods in manually; restore `mods/` from the zip |
| Long "remapping" pause on first boot | Normal — Sinytra Connector is converting the Fabric mods; it's cached afterwards |
