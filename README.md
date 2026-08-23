# CrazyCraft 5.0

A modern revival of the classic CrazyCraft experience on **Minecraft 1.21.1 + NeoForge 21.1.248** — 197 mods, anchored by a from-scratch, line-by-line faithful port of the legendary 1.7.10 **OreSpawn** mod, plus two companion mods built specifically for this pack and a rebuilt **Domestication Innovation**.

Girlfriends, Mobzilla, the King, ant dimensions, uranium everywhere, helicopters, superheroes, lucky blocks, and a world that fights back. Fully playable in English and **Brazilian Portuguese** (complete pt-BR resource pack included).

## Download & install

Grab the latest files from the [**Releases**](../../releases) page:

| File | What it is |
|---|---|
| `CrazyCraft5-Client.zip` | Full client pack, importable into Prism Launcher / MultiMC |
| `CrazyCraft5-Server.zip` | Ready-to-run dedicated server — unzip, double-click, play |

### Client (Prism Launcher / MultiMC — recommended)

1. Install [Prism Launcher](https://prismlauncher.org/) (free) and log in with your Microsoft account.
2. Download `CrazyCraft5-Client.zip` — do **not** unzip it.
3. In Prism: **Add Instance → Import → Browse**, pick the zip, click OK.
4. Edit the instance → **Settings → Java** and allocate **8–12 GB** of RAM (8 GB minimum).
5. Launch. First start takes a few minutes — it's loading 197 mods.

Any launcher that imports MultiMC-format instances works the same way. For a manual install, see [docs/INSTALLING.md](docs/INSTALLING.md).

### Server

1. Install **Java 21** ([Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21)).
2. Unzip `CrazyCraft5-Server.zip` into an empty folder.
3. Run `start.bat` (Windows) or `./start.sh` (Linux/macOS).
4. First run installs NeoForge automatically and asks you to accept the [Minecraft EULA](https://aka.ms/MinecraftEULA) — type `yes`.
5. That's it. The server boots on port `25565`. Give it **6–8 GB** of RAM (the start script defaults to 6 GB; edit the first lines of the script to change it).

Full server details (what was removed for server-side, tuning, troubleshooting): [docs/SERVER.md](docs/SERVER.md).

## What makes this pack different

This is not just a mod list — a large amount of custom engineering went into it:

- **[The OreSpawn 2.0 port](docs/CUSTOM_MODS.md#orespawn-20--the-port)** — the original 1.7.10 OreSpawn, ported to NeoForge 1.21.1 from its original source, driven by a 605-finding parity audit. Every boss, dimension, creature, and item, faithful to the original. [Source repo](https://github.com/CoolFreeze23/Orespawn).
- **[OreSpawn Integrations](docs/CUSTOM_MODS.md#orespawn-integrations)** — a companion mod that weaves OreSpawn into the rest of the pack through five themed content threads (boss economies, uranium industry, the Girlfriend storyline, world rites, big-game combat), an in-game Patchouli guidebook, and compatibility fixes. [Source repo](https://github.com/CoolFreeze23/orespawn-integrations).
- **[OreSpawn Delight](docs/CUSTOM_MODS.md#orespawn-delight)** — a Farmer's Delight kitchen expansion: meats, dishes, and feasts cooked from OreSpawn creatures. [Source repo](https://github.com/CoolFreeze23/orespawn-delight).
- **[Six surgically patched mods](docs/MODIFICATIONS.md#patched-jars)** — crash fixes and compatibility patches applied directly to published jars where no fixed release exists (SecurityCraft × Immersive Portals, Farmer's Respite's broken menu registration, and more).
- **[A complete pt-BR localization](docs/MODIFICATIONS.md#brazilian-portuguese-resource-pack)** — every mod in the pack translated to natural Brazilian Portuguese, shipped as the `CrazyCraft5-ptBR` resource pack (enabled by default).
- **[Curated configs](docs/MODIFICATIONS.md#config-changes)** — combat, performance, and menu configs tuned so everything coexists (including a custom OreSpawn-themed title screen).

## Documentation

| Doc | Contents |
|---|---|
| [docs/CUSTOM_MODS.md](docs/CUSTOM_MODS.md) | Deep dive on the four in-house mods |
| [docs/MODIFICATIONS.md](docs/MODIFICATIONS.md) | Every patched jar, config change, and the pt-BR pack |
| [docs/MODLIST.md](docs/MODLIST.md) | The full 197-mod list with versions and notes |
| [docs/INSTALLING.md](docs/INSTALLING.md) | Manual/alternative install instructions |
| [docs/SERVER.md](docs/SERVER.md) | Server pack internals, tuning, troubleshooting |

## Requirements

| | Minimum | Recommended |
|---|---|---|
| Client RAM | 8 GB allocated | 10–12 GB allocated |
| Server RAM | 6 GB | 8 GB |
| Java | 21 (bundled by Prism) | 21 |

## Credits & legal

All third-party mods belong to their respective authors — see [docs/MODLIST.md](docs/MODLIST.md) for the full roster. OreSpawn was originally created by TheyCallMeDanger; the 2.0 port is an independent fan port. This pack is a non-commercial fan project; if you are a mod author and want your mod removed from the distribution, open an issue and it will be handled immediately.
