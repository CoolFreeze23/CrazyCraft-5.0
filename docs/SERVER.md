# Server files and setup

The server download doesn't contain any mods. It has a small setup program (`crazycraft-loader.jar`), a manifest that says where each mod comes from, start scripts and the pack's configs. On the first start, setup downloads NeoForge and all 163 server mods from their official pages and checks every file before it's used. The pack never hands out other people's files: they only ever come from the people who made them.

## Layout

```
CrazyCraft5-Server/
  CrazyCraft Server Setup.bat   the setup window (Windows)
  startserver.bat / .sh         checks the mods, then starts the server and restarts it after a stop
  crazycraft-loader.jar         the setup program (also: java -jar crazycraft-loader.jar)
  crazycraft-manifest.json      every mod, where it's downloaded from, and its checksum
  user_jvm_args.txt             memory (-Xmx) and Java flags
  user-mods.txt                 mods you add yourself, so setup leaves them alone
  server.properties             defaults explained below
  config/  defaultconfigs/      same as the client
  mcheli/                       the uranium A-10 content pack
  moonlight-global-datapacks/
  CREDITS.txt                   every mod and its official page
  README.txt
```

After setup, the folder also has `libraries/` (NeoForge), `mods/` and a hidden `.crazycraft/` folder with the setup log and what it installed.

## What setup does

1. Checks Java. Minecraft 1.21.1 needs Java 21.
2. Installs NeoForge 21.1.248 if it isn't there yet. The installer comes from maven.neoforged.net and is checked against its published SHA-1.
3. Checks every mod in `mods/` against the manifest, and downloads whatever is missing or doesn't match, six at a time:
   - 150 mods come from Modrinth's CDN and 6 from CurseForge (only mods whose authors allow downloads outside CurseForge).
   - Our own 6 mods come from their GitHub releases.
   - Six of the server's mods need a small fix to run in the pack (see [MODIFICATIONS.md](MODIFICATIONS.md#patched-jars)). Setup downloads the untouched original and applies the fix; the result is checked too.
   - One mod, MCHeli, can only be downloaded from CurseForge itself. Setup shows its download page and picks the file up from `manual/` or your Downloads folder, checking it before use.
4. Copies the extra files the pack builds from a mod's own jar (the uranium A-10 uses MCHeli's A-10 model).
5. Moves jars that are no longer part of the pack to `mods-removed/<date>/`. It never deletes anything. Jars listed in `user-mods.txt` are left alone.
6. Asks about the [Minecraft EULA](https://aka.ms/MinecraftEULA) if it hasn't been accepted yet.

When nothing changed, the whole check takes about a second, so the start scripts run it before every start. A download that stalls or fails is retried up to four times, and a file that doesn't match its checksum is never used.

## Setup window or terminal

- **Window:** double-click `CrazyCraft Server Setup.bat`, or run `java -jar crazycraft-loader.jar` on a machine with a screen. It shows the steps and progress, lets you set the server's memory and accept the EULA, handles the MCHeli download (open the page, then it spots the file by itself, or drop the file on the window), and has a Start server button. "Mods & credits" lists every mod with a link to its page.
- **Terminal:** `startserver.bat` / `./startserver.sh` run the same setup in the console first. On a server without a screen, `java -jar crazycraft-loader.jar --nogui` does the setup alone.

Options for the start scripts, as environment variables:

| Variable | Effect |
|---|---|
| `CRAZYCRAFT_JAVA` | Full path to the Java 21 to use |
| `CRAZYCRAFT_RESTART=false` | Stop instead of restarting after the server stops or crashes |
| `CRAZYCRAFT_INSTALL_ONLY=true` | Set up or update, then exit |
| `CRAZYCRAFT_SKIP_SETUP=true` | Start without checking the mods |
| `CRAZYCRAFT_ACCEPT_EULA=true` | Accept the Minecraft EULA without asking |

The loader itself takes `--nogui`, `--dir <folder>`, `--accept-eula` and `--memory <GB>`.

## Updating

Download the new server files, unzip them over the server folder and start as usual. Setup downloads only what changed and moves the old jars to `mods-removed/`. The world, `server.properties`, whitelist and ops stay as they are.

## Client-only mods

Of the pack's 202 mods, 39 are client-side only (rendering, HUD, menus, audio, cosmetics) and aren't installed on the server. The manifest marks them `"side": "client"`. Players connect with the normal client pack.

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
| A download fails | Usually a flaky connection. Run setup again; files that are already in place are kept |
| Setup waits for MCHeli | Download it from the page setup shows and save it into `manual/` or your Downloads folder |
| `You need to agree to the EULA` | Run setup again and accept, or set `eula=true` in `eula.txt` |
| A mod of your own disappeared from `mods/` | It's in `mods-removed/`. Move it back and list its file name in `user-mods.txt` |
| Long "remapping" pause on first boot | Normal. Sinytra Connector is converting the Fabric mods, and the result is cached after that |

## For pack maintainers

- `pack/sources.json` names the sources that can't be looked up automatically: our own mods on GitHub, the patched mods' originals, and extra files.
- `python scripts/gen_manifest.py --version <x.y.z> --check-urls` builds `pack/manifest.json` from the instance's `mods/`. It finds each jar on Modrinth by SHA-512 and on CurseForge by fingerprint, works out the patched mods' edits by comparing them with their originals (and checks that the edits rebuild the pack's jar exactly), and checks that every download link answers.
- `python loader/build.py` builds `build/loader/crazycraft-loader.jar` with the manifest inside.
- `python scripts/build_packs.py server` builds the server files and `CrazyCraft5-Server.zip`; `python scripts/build_packs.py assemble` installs a complete server into `build/server-pack` with the loader itself, for boot tests.
- `python scripts/build_packs.py client` builds `CrazyCraft5-Client.zip`: the Prism instance with the pack's configs and its own resource pack, the loader and the manifest in `.minecraft/`, and `crazycraft-loader.jar --client` as the instance's pre-launch command. In client mode the loader installs the mods and resource packs (no NeoForge, which the launcher installs), exits at once when everything is in place, and only moves jars it installed itself, so mods a player adds stay put.
