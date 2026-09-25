# Installing: all methods

## Method 1: Prism Launcher / MultiMC import (recommended)

1. Install [Prism Launcher](https://prismlauncher.org/) and sign in with your Microsoft account.
2. Download `CrazyCraft5-Client.zip` from [Releases](../../../releases). Keep it zipped.
3. Click Add Instance > Import > Browse, select the zip and click OK.
4. Right-click the instance, go to Settings > Java > Memory and set the maximum to 8192 MB or more.
5. Launch.

## Method 2: Manual install into any launcher

Works with the vanilla launcher with NeoForge installed by hand, ATLauncher and others.

1. Install NeoForge 21.1.248 for Minecraft 1.21.1 from [neoforged.net](https://neoforged.net/) (pick version 21.1.248 in the installer).
2. Download `CrazyCraft5-Client.zip` and open it. Inside `.minecraft/` you'll find `mods/`, `config/`, `resourcepacks/` and a few support folders.
3. Copy all of those folders into your game directory (the folder that has your `saves/` in it). For a clean profile, make a new game directory.
4. Launch the NeoForge 1.21.1 profile with 8 GB of RAM or more (`-Xmx8G` in the JVM arguments).

## First launch

- The first boot takes a lot longer than vanilla (mod loading, plus Connector remapping the Fabric mods). Later boots are faster because of caching.
- The title screen is custom. If you see the OreSpawn-themed screen, everything loaded.
- The pack ships with the Brazilian Portuguese resource pack turned on. To play in English, go to Options > Resource Packs and disable `CrazyCraft5-ptBR`.

## Updating

Releases are cumulative. To update, import the new client zip as a new instance, then copy your `saves/` folder over from the old instance. Your worlds are never inside the pack zip.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Crash on startup with less than 8 GB | Allocate more RAM. 202 mods really do need it |
| "Out of memory" during world gen | Raise the allocation to 10 to 12 GB |
| Missing textures / English text everywhere | The resource packs got disabled. Re-enable `CrazyCraft5-ptBR` (or leave it off if you want English) |
| Fabric mod errors mentioning "Connector" | Delete the `.connector` folder inside the instance and relaunch (this clears the remap cache) |
