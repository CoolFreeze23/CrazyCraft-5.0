# Everything that was modified

Apart from the five custom mods, several published mods and configs were changed to make the pack stable and make everything work together. This page lists all of them.

## Patched jars

Seven published mods need a small fix to work in the pack, because no fixed upstream release exists. Each fix is the smallest change that works (usually one mixin entry or a few bytes). The server setup downloads the untouched original from its official page and applies the fix on install; `pack/manifest.json` lists every edit, and the result is checked before it's used. `scripts/gen_manifest.py` works the edits out by comparing the pack's jar with the original.

| Mod | Patch | Why |
|---|---|---|
| SecurityCraft 1.10.2.1 | Removed its camera `ChunkMap` mixin | Immersive Portals (required by the Portal Gun) rewrites `ChunkMap.onChunkReadyToSend`, and SecurityCraft's camera mixin hard-crashes on world load when it can't hook it. The only side effect: camera monitors won't force-load far-away chunks. |
| Farmer's Respite 3.0.1 (`-dyefix`) | Replaced `"item"` with `"id"` in its red dye recipe | 3.0.1 fixed the duplicate kettle registration upstream, so only the recipe fix remains: its red dye recipe still uses the pre-1.21 result syntax, which the game refuses to load. |
| Ars Nouveau 5.13.2 | Stripped the embedded `lambdynamiclights-api` | The bundled API stub conflicted with this pack's mod set. |
| Randomizer Complete Edition v0.6 | Fixed its `crafting_table` recipe file | The datapack uses newer recipe JSON syntax that 1.21.1 can't parse, so crafting tables could become uncraftable when randomized. |
| Mob Mutator 1.0.0 | Removed its `TitleScreenMixin` from the mixin config | It forces an "editor" button onto the title screen that the layout can't hide. Removing the mixin only removes the button; all the gameplay mixins are intact. |
| FancyMenu 3.9.14 | One-byte patch to the `isCopyrightButton()` check | FancyMenu deliberately stops packs from hiding the Mojang copyright line. The patch lets the pack's custom title screen control the whole layout. |
| Structory: Towers 1.0.17 | Added `modLoader="lowcodefml"` to its `neoforge.mods.toml` | It's a data-only mod without a mod class, and NeoForge 1.21.1 only loads such a jar when it says so. |

> Note for updaters: if you ever update one of these seven mods, the fix has to be made again and `pack/sources.json` pointed at the new original (the manifest generator then works out the edits). Everything else in `mods/` is stock.

## Deliberate version pins

| Mods | Why pinned |
|---|---|
| Sinytra Connector 2.0.0-beta.16 and Forgified Fabric API 0.116.15 | These two are released as a matched pair and have to be upgraded together. This pair is the oldest one that provides `ServerPlayerEvents.JOIN/LEAVE`, which Soul Shards Despawn needs. |
| YUNG's API 5.1.7 | 5.1.6 had a `NullPointerException` in `EnhancedBeardifierHelper.computeDensity` during chunk generation (it showed up with ModernFix's worldgen allocation optimization). 5.1.7 plus the guard mixin in OreSpawn Integrations covers it from both sides. |
| GeckoLib 4.9.2, playerAnimator 2.0.4 | The pack once carried two copies of each. The older duplicates are in the `disabled-mods/` history and must not come back. |

## Config changes

All of these ship in `config/`. The notable ones:

| File | Change | Why |
|---|---|---|
| `bettercombat/fallback_compatibility.json` | Portal Gun items added to `blacklist_item_id_regex` | Better Combat was catching left-click on the Portal Gun as a melee swing, which ate the portal shot. |
| `cristellib/auto_config_settings.json5` | `orespawn` and `multihitboxlib` added to `blacklistedMods` | Cristel Lib writes its own copy of each mod's structure spacing into `config/cristellib/<mod>/` and applies that copy over the mod's data. It only refreshes the copy when a mod adds or renames a structure set, so an OreSpawn update that only changes how often its structures spawn would be quietly undone. MultiHitboxLib ships inside the OreSpawn jar, so Cristel Lib made a second copy of OreSpawn's structures under its name. With both on the blacklist, OreSpawn's structures always spawn at the rates it ships with, which follow the 1.7.10 original. |
| `modernfix-mixins.properties` | `mixin.perf.worldgen_allocation=false` | This optimization reuses worldgen objects in a way that exposed the YUNG's API null-field crash (see the pins above). It's off for stability; everything else in ModernFix stays on. |
| `punchy/punchy_config.json` | OreSpawn's eight custom-rendered weapons added to `itemBlacklist` | Punchy re-renders held items on a visible-hands rig, which mangles the giant weapons that draw themselves (Big Bertha, the Royal Guardian Sword, the chainsaw and the rest). Blacklisting them keeps their own rendering. |
| `immediatelyfast.json` | `hud_batching` set to `false` | With HUD batching on, ImmediatelyFast holds the HUD's drawing back while Auto HUD draws the crosshair and its fading parts into a framebuffer of its own. The crosshair then came out plain white or not at all, and the hotbar items and XP number stopped fading. With it off, both work as before, and frame rates were the same in our tests. Everything else in ImmediatelyFast stays at its defaults. |
| `fancymenu/` | Full custom title screen | OreSpawn-themed key art, a custom logo and a trimmed button stack. This is the pack's title screen, and it relies on the FancyMenu one-byte patch above. |

## Brazilian Portuguese resource pack

`resourcepacks/CrazyCraft5-ptBR.zip` is a complete pt-BR localization of the pack, turned on by default in the shipped `options.txt`.

- It covers every mod that ships English text and had no official pt-BR translation. That's most of the pack, including the three OreSpawn-family mods, Twilight Forest, the Aether, Mowzie's Mobs, DoggyTalents, ProjectE, SecurityCraft, the delight-family food mods and dozens more.
- Jokes, puns and pop-culture references are adapted for Brazilian players instead of translated word for word, and vanilla things keep their official Minecraft pt-BR names.
- All Minecraft formatting codes (`§a`, `%s`, Patchouli `$(...)` macros) are kept intact and checked with a script.
- Neo Origins ships no pt-BR at all, so the pack carries a full translation of its 2,296 strings: every built-in origin and class, all powers and evolution tiers, the picker and HUD editor screens, keybinds, config labels and command feedback. Its HUD resource-bar labels ("Energy", "Essence", "Stamina" and so on) are raw strings the mod never translates, so OreSpawn Integrations (since 0.6.1) swaps them in on the client.
- Three mods (Mowzie's Mobs, Serene Seasons, FancyToasts) ship broken language JSON files inside their jars that Minecraft refuses to parse. Their translations go through a `crazycraft_ptbr` namespace inside the pack instead, so they work anyway.

Domestication Innovation and Monster Hunter Villager are the exceptions: both ship their own pt-BR translation inside the jar, so they read correctly with or without the pack.

To play in English, just disable the resource pack (Options > Resource Packs).

## Other resource packs

The pack's other 29 resource packs come from Modrinth and CurseForge: the client setup downloads them on the first launch, like the mods. Six packs from earlier versions are no longer in the pack, because none of them has an official download the setup may use:

| Pack | Why it's out |
|---|---|
| Actually 3D Stuff | The build the pack used is no longer published; the author's current versions are for Minecraft 1.21.9 and newer. It was on by default |
| 3D Mace, the Actions & Stuff pumpkins, HMI Alittle Axolotl | Not published on Modrinth or CurseForge |
| Better Illagers FA, Better Trident | Their authors only allow downloads from CurseForge itself |

FA+ Emissive and FA+ Objects are the same versions as before, now taken from Modrinth; only the terms text inside them differs from the CurseForge files.

## Instance-level tweaks

- `options.txt` ships minimal: default keybinds and the pt-BR pack turned on. Everything else is generated on first launch.
- The instance allocates 8 GB by default (raise it to 10 to 12 GB if you have 32 GB of RAM).
- JourneyMap data, world saves, logs and other personal data are not part of the download.

## Versions held back

The pack tracks the newest 1.21.1 build of every mod (`scripts/check_updates.py` reports what is behind, `scripts/update_mods.py` moves the instance). A few are held on purpose, listed with their reason in `pack/pins.json`:

- **Sodium 0.6.13**: Iris 1.8.x for 1.21.1 pins it; the 0.8 line has no matching Iris.
- **Cosy Critters & Creepy Crawlies 0.0.1a**: 0.3.x's `ClientLevel` mixin targets a method Connector cannot map on NeoForge, so the client exits on startup.
- **Subtle Effects 1.9.4**: 1.14.x's End Remastered hook calls a method End Remastered 6.3.0 for 1.21.1 does not have, so the client fails to load.
- **Fresh Food 1.0**: 1.3.x needs Respackopts, whose NeoForge build asks for LibJF modules the NeoForge LibJF does not provide.
