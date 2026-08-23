# Everything that was modified

Beyond the four in-house mods, several published mods and configs were modified to make the pack stable and coherent. This page is the complete record.

## Patched jars

Six published mods are shipped as locally patched jars, because no fixed upstream release exists. In each case the patch is the *smallest possible change* (usually one mixin entry or a few bytes), and the untouched original is what you would get by re-downloading the mod from its official page.

| Mod | Patch | Why |
|---|---|---|
| **SecurityCraft** 1.10.1 | Removed its camera `ChunkMap` mixin | Immersive Portals (required by the Portal Gun) rewrites `ChunkMap.onChunkReadyToSend`; SecurityCraft's camera mixin hard-crashes on world load when it can't hook it. Only side effect: camera monitors won't force-load far-away chunks. |
| **Farmer's Respite** 3.0.0 (`-menufix`) | NOP'd a 7-byte duplicate registration call | The only published 1.21.1 port registers its `farmersrespite:kettle` menu type **twice**, which crashes deterministically in any pack. The patch removes the leftover duplicate; the kettle works normally. |
| **Ars Nouveau** 5.13.0 | Stripped the embedded `lambdynamiclights-api` | The bundled API stub conflicted in this pack's mod set. |
| **Randomizer Complete Edition** v0.6 | Fixed its `crafting_table` recipe file | The datapack uses newer recipe JSON syntax that 1.21.1 can't parse — meaning crafting tables could become uncraftable when randomized. |
| **Mob Mutator** 1.0.0 | Removed its `TitleScreenMixin` from the mixin config | It force-injects an "editor" button onto the title screen that can't be hidden by layout; removing the mixin only removes the button, all gameplay mixins intact. |
| **FancyMenu** 3.9.7 | One-byte patch to `isCopyrightButton()` gating | FancyMenu deliberately prevents packs from hiding the Mojang copyright line; the patch allows the pack's custom title screen to fully control the layout. |

> Note for updaters: if you ever update one of these six mods, the same patch has to be reapplied (or the fix accepted as lost). Everything else in `mods/` is stock.

## Deliberate version pins

| Mods | Why pinned |
|---|---|
| **Sinytra Connector 2.0.0-beta.16** + **Forgified Fabric API 0.116.15** | These two release as a matched pair and must be upgraded together. This pair is the minimum that provides `ServerPlayerEvents.JOIN/LEAVE`, which Soul Shards Despawn needs. |
| **YUNG's API 5.1.7** | 5.1.6 had a `NullPointerException` in `EnhancedBeardifierHelper.computeDensity` during chunk generation (surfaced with ModernFix's worldgen allocation optimization). 5.1.7 + the guard mixin in OreSpawn Integrations covers it from both sides. |
| **GeckoLib 4.9.2**, **playerAnimator 2.0.4** | The pack once carried two copies of each; the older duplicates live in `disabled-mods/` history and must not come back. |

## Config changes

All shipped in `config/` — the notable ones:

| File | Change | Why |
|---|---|---|
| `bettercombat/fallback_compatibility.json` | Portal Gun items added to `blacklist_item_id_regex` | Better Combat was capturing left-click on the Portal Gun as a melee swing, eating the portal shot. |
| `modernfix-mixins.properties` | `mixin.perf.worldgen_allocation=false` | This optimization reuses worldgen objects in a way that exposed the YUNG's API null-field crash (see pins above). Disabled for stability; everything else in ModernFix stays on. |
| `punchy/punchy_config.json` | OreSpawn's eight custom-rendered weapons added to `itemBlacklist` | Punchy re-renders held items on a visible-hands rig, which mangles the giant weapons (Big Bertha, the Royal Guardian Sword, the chainsaw...) that draw themselves. Blacklisting keeps their custom rendering. |
| `fancymenu/…` | Full custom title screen | OreSpawn-themed key art, custom logo, trimmed button stack — the pack's identity screen. Works together with the FancyMenu one-byte patch above. |

## Brazilian Portuguese resource pack

`resourcepacks/CrazyCraft5-ptBR.zip` — a complete pt-BR localization of the pack, enabled by default in the shipped `options.txt`.

- Covers **every mod** that ships English text and lacked an official pt-BR translation (the vast majority of the pack, including the three OreSpawn-family mods, Twilight Forest, the Aether, Mowzie's Mobs, DoggyTalents, ProjectE, SecurityCraft, the delight-family food mods, and dozens more).
- Native localization, not literal translation: jokes, puns, and pop-culture references are adapted for a Brazilian audience, while official Minecraft pt-BR terminology is kept for vanilla concepts.
- All Minecraft formatting codes (`§a`, `%s`, Patchouli `$(...)` macros) are preserved and machine-verified.
- Three mods (Mowzie's Mobs, Serene Seasons, FancyToasts) ship *malformed* language JSONs inside their jars that Minecraft refuses to parse; their translations are rerouted through a `crazycraft_ptbr` namespace inside the pack so they work anyway.

Domestication Innovation is the exception: it ships its own pt-BR translation inside the jar, so it reads correctly with or without the pack.

To play in English: just disable the resource pack (Options → Resource Packs).

## Instance-level tweaks

- `options.txt` ships minimal: default keybinds, pt-BR pack enabled. Everything else regenerates on first launch.
- The instance allocates 8 GB by default (raise to 10–12 GB if you have 32 GB of RAM).
- JourneyMap, world saves, logs, and other personal data are not part of the distribution.
