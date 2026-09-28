# CrazyCraft 5.0

A revival of the classic CrazyCraft pack for Minecraft 1.21.1 and NeoForge 21.1.248, with 202 mods. The heart of it is a port of the 1.7.10 OreSpawn mod, done from the original source code, along with two companion mods written for this pack and our NeoForge ports of Domestication Innovation and Monster Hunter Villager.

Girlfriends, Mobzilla, the King, ant dimensions, uranium everywhere, helicopters, superheroes and lucky blocks. You can play the whole pack in English or Brazilian Portuguese (a complete pt-BR resource pack is included).

## Download & install

Get the latest files from the [Releases](../../releases) page:

| File | What it is |
|---|---|
| `CrazyCraft5-Client.zip` | Full client pack, importable into Prism Launcher or MultiMC |
| `CrazyCraft5-Server.zip` | Dedicated server files. The setup downloads every mod from its official page. |

### Client (Prism Launcher or MultiMC, recommended)

1. Install [Prism Launcher](https://prismlauncher.org/) (free) and log in with your Microsoft account.
2. Download `CrazyCraft5-Client.zip`. Don't unzip it.
3. In Prism, go to Add Instance > Import > Browse, pick the zip and click OK.
4. Edit the instance, open Settings > Java and give it 8 to 12 GB of RAM (8 GB minimum).
5. Launch. The first start takes a few minutes while all the mods load.

Any launcher that imports MultiMC-format instances works the same way. For a manual install, see [docs/INSTALLING.md](docs/INSTALLING.md).

### Server

1. Install Java 21 ([Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21)).
2. Unzip `CrazyCraft5-Server.zip` into an empty folder.
3. Windows: double-click `CrazyCraft Server Setup.bat`. Linux/macOS or a server without a screen: `./startserver.sh`.
4. Setup installs NeoForge and downloads all 163 server mods from their official pages (Modrinth, CurseForge and our own GitHub releases), checking every file. It asks how much memory the server gets and about the [Minecraft EULA](https://aka.ms/MinecraftEULA).
5. One mod, MCHeli, can only be downloaded from CurseForge itself. Setup shows you the page and picks up the file from your Downloads folder.
6. That's it. Start the server with `startserver.bat` / `./startserver.sh`; it checks the mods first and restarts the server if it stops. The server runs on port `25565` with 6 to 8 GB of RAM.

How setup works, updating, tuning and troubleshooting are in [docs/SERVER.md](docs/SERVER.md).

## What makes this pack different

Besides the mod list itself, a lot of custom work went into the pack:

- [OreSpawn 2.0](docs/CUSTOM_MODS.md#orespawn-20-port), the original 1.7.10 OreSpawn ported to NeoForge 1.21.1 from its source code and checked against it in an audit that logged 605 findings. Every boss, dimension, creature and item from the original is in. [Source repo](https://github.com/CoolFreeze23/Orespawn).
- [OreSpawn Integrations](docs/CUSTOM_MODS.md#orespawn-integrations), a companion mod that ties OreSpawn into the rest of the pack. It has five content threads (boss economies, uranium industry, the Girlfriend storyline, world rites, big-game combat), an in-game Patchouli guidebook and some compatibility fixes. With Monster Hunter Villager installed, it also sends the Monster Hunters after some of OreSpawn's creatures. [Source repo](https://github.com/CoolFreeze23/orespawn-integrations).
- [OreSpawn Delight](docs/CUSTOM_MODS.md#orespawn-delight), a Farmer's Delight add-on with meats, dishes and feasts cooked from OreSpawn creatures. [Source repo](https://github.com/CoolFreeze23/orespawn-delight).
- [Domestication Innovation](docs/CUSTOM_MODS.md#domestication-innovation-port), alex_the_668's pet overhaul, ported from Forge 1.20.1 to NeoForge 1.21.1, with new collar enchantments and datapack taming added. [Source repo](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1).
- [Monster Hunter Villager](docs/CUSTOM_MODS.md#monster-hunter-villager-port), our NeoForge 1.21.1 port of Yoshi's Forge 1.20.1 mod. It adds a villager that hunts monsters with thrown traps and a knife. The original ran its hunter and trap code on every mob in the world every tick; the port only runs it for Monster Hunters and placed traps. [Source repo](https://github.com/CoolFreeze23/monster-hunter-villager-neoforge).
- [Patched jars](docs/MODIFICATIONS.md#patched-jars) for mods with no fixed release. The server setup downloads the published jars and applies the crash fixes and compatibility patches on install (SecurityCraft with Immersive Portals, Farmer's Respite's broken menu registration, and more).
- [A full pt-BR translation](docs/MODIFICATIONS.md#brazilian-portuguese-resource-pack) of every mod in the pack, shipped as the `CrazyCraft5-ptBR` resource pack and turned on by default.
- [Config changes](docs/MODIFICATIONS.md#config-changes) to combat, performance and menus so everything works together, plus a custom OreSpawn title screen.

## Documentation

| Doc | Contents |
|---|---|
| [docs/CUSTOM_MODS.md](docs/CUSTOM_MODS.md) | The mods we maintain ourselves, in detail |
| [docs/MODIFICATIONS.md](docs/MODIFICATIONS.md) | Every patched jar, config change, and the pt-BR pack |
| [docs/MODLIST.md](docs/MODLIST.md) | The full mod list with versions and notes |
| [docs/INSTALLING.md](docs/INSTALLING.md) | Manual and other install methods |
| [docs/SERVER.md](docs/SERVER.md) | Server setup, updating, tuning, troubleshooting |

## Requirements

| | Minimum | Recommended |
|---|---|---|
| Client RAM | 8 GB allocated | 10 to 12 GB allocated |
| Server RAM | 6 GB | 8 GB |
| Java | 21 (bundled by Prism) | 21 |

## Credits & legal

All third-party mods belong to their authors. [docs/MODLIST.md](docs/MODLIST.md) lists every one of them. The server files don't include anyone's mods: the setup downloads each one from its official page, and `pack/manifest.json` shows exactly where every file comes from. OreSpawn was originally made by TheyCallMeDanger, and the 2.0 port is an independent fan port. Domestication Innovation is by alex_the_668 and Monster Hunter Villager is by Yoshi; the versions here are our NeoForge 1.21.1 ports. This pack is a non-commercial fan project. If you're a mod author and want your mod taken out of the pack, open an issue and we'll take it out right away.
