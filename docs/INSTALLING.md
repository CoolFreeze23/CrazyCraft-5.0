# Installing — all methods

## Method 1: Prism Launcher / MultiMC import (recommended)

1. Install [Prism Launcher](https://prismlauncher.org/) and sign in with your Microsoft account.
2. Download `CrazyCraft5-Client.zip` from [Releases](../../../releases) — keep it zipped.
3. **Add Instance → Import → Browse** → select the zip → **OK**.
4. Right-click the instance → **Settings → Java → Memory** → set maximum to **8192 MB** or more.
5. Launch.

## Method 2: Manual install into any launcher

Works with the vanilla launcher + manually installed NeoForge, ATLauncher, etc.

1. Install **NeoForge 21.1.248** for Minecraft 1.21.1 from [neoforged.net](https://neoforged.net/) (pick version 21.1.248 in the installer).
2. Download `CrazyCraft5-Client.zip` and open it. Inside `.minecraft/` you'll find `mods/`, `config/`, `resourcepacks/`, and a few support folders.
3. Copy all of those folders into your game directory (the folder that contains your `saves/` — for a clean profile, make a fresh game directory).
4. Launch the NeoForge 1.21.1 profile with **8+ GB** of RAM (`-Xmx8G` in JVM arguments).

## First launch

- First boot takes noticeably longer than vanilla (mod loading + Connector remapping the Fabric mods). Later boots are faster thanks to caching.
- The title screen is custom — if you see the OreSpawn-themed screen, everything loaded.
- The pack ships with the Brazilian Portuguese resource pack enabled. To play in English: **Options → Resource Packs** → disable `CrazyCraft5-ptBR`.

## Updating

Releases are cumulative — to update, import the new client zip as a new instance, then copy your `saves/` folder over from the old instance. Your worlds are never inside the pack zip.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Crash on startup with < 8 GB | Allocate more RAM — 193 mods genuinely need it |
| "Out of memory" during world gen | Raise allocation to 10–12 GB |
| Missing textures / English text everywhere | Resource packs got disabled — re-enable `CrazyCraft5-ptBR` (or ignore if you want English) |
| Fabric mod errors mentioning "Connector" | Delete the `.connector` folder inside the instance and relaunch (clears the remap cache) |
