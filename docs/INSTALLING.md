# Installing: all methods

The client download holds the pack's settings, configs and its own resource pack, plus a small setup program (`crazycraft-loader.jar`). It doesn't include anyone else's mods or resource packs: the setup downloads each one from its official page (Modrinth, CurseForge or the author's GitHub) the first time you play, and checks every file before it's used.

## Method 1: Prism Launcher / MultiMC import (recommended)

1. Install [Prism Launcher](https://prismlauncher.org/) and sign in with your Microsoft account.
2. Download `CrazyCraft5-Client.zip` from [Releases](../../../releases). Keep it zipped.
3. Click Add Instance > Import > Browse, select the zip and click OK.
4. Right-click the instance, go to Settings > Java > Memory and set the maximum to 8192 MB or more.
5. Launch. The first launch opens the CrazyCraft setup window, which downloads the 202 mods and 29 resource packs (about 960 MB) and then starts the game. Later launches only check the files, which takes about a second, and don't show the window.

The instance runs the setup as its pre-launch command (Edit instance > Settings > Custom commands), so leave that in place.

### One mod by hand: MCHeli

MCHeli's author only allows downloads from CurseForge itself, so the setup can't fetch it. The setup window shows an "Open download page" button: download the file there, and the setup finds it in your Downloads folder by itself (you can also drop the file on the window). You only do this once.

## Method 2: Manual install into any launcher

Works with the vanilla launcher with NeoForge installed by hand, ATLauncher and others.

1. Install NeoForge 21.1.248 for Minecraft 1.21.1 from [neoforged.net](https://neoforged.net/) (pick version 21.1.248 in the installer).
2. Download `CrazyCraft5-Client.zip` and open it. Copy everything inside its `.minecraft/` folder into your game directory (the folder that has your `saves/` in it). For a clean profile, make a new game directory.
3. In that game directory, run `java -jar crazycraft-loader.jar --client` (Java 21). The setup window downloads the mods and resource packs into the game directory.
4. Launch the NeoForge 1.21.1 profile with 8 GB of RAM or more (`-Xmx8G` in the JVM arguments). Run the setup again after changing the pack's files; it only fetches what's missing.

## First launch

- The first boot takes a lot longer than vanilla (mod loading, plus Connector remapping the Fabric mods). Later boots are faster because of caching.
- The title screen is custom. If you see the OreSpawn-themed screen, everything loaded.
- The pack ships with the Brazilian Portuguese resource pack turned on. To play in English, go to Options > Resource Packs and disable `CrazyCraft5-ptBR`.

## Updating

To update, import the new client zip as a new instance, then copy your `saves/` folder over from the old instance. Your worlds are never inside the pack zip.

## Troubleshooting

| Symptom | Fix |
|---|---|
| The setup window says a download failed | Usually a flaky connection. Press Try again; files that are already in place are kept |
| The setup waits for MCHeli | Download it from the page the window opens and save it into your Downloads folder, or drop it on the window |
| Crash on startup with less than 8 GB | Allocate more RAM. 202 mods really do need it |
| "Out of memory" during world gen | Raise the allocation to 10 to 12 GB |
| Missing textures / English text everywhere | The resource packs got disabled. Re-enable `CrazyCraft5-ptBR` (or leave it off if you want English) |
| Fabric mod errors mentioning "Connector" | Delete the `.connector` folder inside the instance and relaunch (this clears the remap cache) |
