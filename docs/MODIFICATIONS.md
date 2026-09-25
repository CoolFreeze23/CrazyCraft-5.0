# Everything that was modified

Apart from the five custom mods, several published mods and configs were changed to make the pack stable and make everything work together. This page lists all of them.

## Patched jars

Six published mods ship as locally patched jars, because no fixed upstream release exists. Each patch is the smallest change that works (usually one mixin entry or a few bytes), and the untouched original is what you get if you download the mod again from its official page.

| Mod | Patch | Why |
|---|---|---|
| SecurityCraft 1.10.1 | Removed its camera `ChunkMap` mixin | Immersive Portals (required by the Portal Gun) rewrites `ChunkMap.onChunkReadyToSend`, and SecurityCraft's camera mixin hard-crashes on world load when it can't hook it. The only side effect: camera monitors won't force-load far-away chunks. |
| Farmer's Respite 3.0.0 (`-menufix`) | Replaced a 7-byte duplicate registration call with NOPs | The only published 1.21.1 port registers its `farmersrespite:kettle` menu type twice, which crashes every time, in any pack. The patch removes the leftover duplicate and the kettle works normally. |
| Ars Nouveau 5.13.0 | Stripped the embedded `lambdynamiclights-api` | The bundled API stub conflicted with this pack's mod set. |
| Randomizer Complete Edition v0.6 | Fixed its `crafting_table` recipe file | The datapack uses newer recipe JSON syntax that 1.21.1 can't parse, so crafting tables could become uncraftable when randomized. |
| Mob Mutator 1.0.0 | Removed its `TitleScreenMixin` from the mixin config | It forces an "editor" button onto the title screen that the layout can't hide. Removing the mixin only removes the button; all the gameplay mixins are intact. |
| FancyMenu 3.9.7 | One-byte patch to the `isCopyrightButton()` check | FancyMenu deliberately stops packs from hiding the Mojang copyright line. The patch lets the pack's custom title screen control the whole layout. |

> Note for updaters: if you ever update one of these six mods, the same patch has to be applied again (or the fix is lost). Everything else in `mods/` is stock.

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
| `modernfix-mixins.properties` | `mixin.perf.worldgen_allocation=false` | This optimization reuses worldgen objects in a way that exposed the YUNG's API null-field crash (see the pins above). It's off for stability; everything else in ModernFix stays on. |
| `punchy/punchy_config.json` | OreSpawn's eight custom-rendered weapons added to `itemBlacklist` | Punchy re-renders held items on a visible-hands rig, which mangles the giant weapons that draw themselves (Big Bertha, the Royal Guardian Sword, the chainsaw and the rest). Blacklisting them keeps their own rendering. |
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

## Instance-level tweaks

- `options.txt` ships minimal: default keybinds and the pt-BR pack turned on. Everything else is generated on first launch.
- The instance allocates 8 GB by default (raise it to 10 to 12 GB if you have 32 GB of RAM).
- JourneyMap data, world saves, logs and other personal data are not part of the download.
