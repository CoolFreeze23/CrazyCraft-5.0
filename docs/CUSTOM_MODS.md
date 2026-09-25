# The custom mods

We maintain these mods ourselves. Two were written for it (OreSpawn Integrations and OreSpawn Delight), and three are ports we keep on NeoForge 1.21.1 (OreSpawn, Domestication Innovation and Monster Hunter Villager). All of them are open source under the same GitHub account, and each one ships as a normal jar in `mods/`.

---

## OreSpawn 2.0 port

- Jar: `orespawn-1.21.1-2.0.0-beta.x.jar`
- Source: [CoolFreeze23/Orespawn](https://github.com/CoolFreeze23/Orespawn)
- Releases: [CoolFreeze23/Orespawn/releases](https://github.com/CoolFreeze23/Orespawn/releases)

This is the main mod of the pack. OreSpawn only ever existed for Minecraft 1.7.10, so it was ported to NeoForge 1.21.1 from the original source code. The goal was to match the original, so there's no new content or "modernized" balance, and nothing the original didn't do.

How it was done (the repo's changelog has the details):

- The original 1.7.10 source lives in the repo under `reference_1_7_10_source/`, and every ported system cites the original file and line it came from.
- An audit of the original turned up 605 findings. Each one has its own ID, and they were worked through in phases.
- Every fix is summarized in the changelog, and so are the few places where the port deliberately differs from the original.
- About 25 hardcoded 1.7.10 structures were moved to modern worldgen using a documented conversion pattern. The SpawnOres block pool (about 105 block types) and the 116 water-bucket egg recipes were reproduced exactly.

In game you get the Crystal, Mining, Village and Ant dimensions, the full boss ladder (Mobzilla, the King, the Queen, the Kraken, the Emperor Scorpion, the Basilisc and more), Girlfriends and Boyfriends, Big Bertha and the Royal Guardian set, uranium and titanium ore progression, ant nests, dungeons, and everything else the 1.7.10 version had.

Some of the fixes that went in while this pack was being built (see the repo's `CHANGELOG.md`):

- Ants and termites were reworked. The port had given ants natural biome spawning that the original never had, so that was removed. The original despawn behavior, nest spawn caps, the termites' replicate-on-eat behavior, biome-tinted anthill blocks and the original 0.25x and 0.35x render scales are all back.
- The Mining dimension had been set up as a dark cavern with a ceiling by mistake. It's back to the original's open-sky surface world with daylight.
- OreSpawn's lake feature could crash chunk generation when Serene Seasons redirected the vanilla freeze check into a biome lookup outside the region being generated. The freeze check is now done inline and stays inside the region.

## OreSpawn Integrations

- Jar: `orespawn_integrations-0.9.x.jar`
- Source: [CoolFreeze23/orespawn-integrations](https://github.com/CoolFreeze23/orespawn-integrations)

A companion mod written for this pack so OreSpawn fits in with the rest of the mods instead of feeling bolted on. Apart from OreSpawn itself, it has no hard dependencies. Every integration checks whether the other mod is loaded, so removing any of them breaks nothing, and each thread can be turned off in the config.

The content is split into five threads:

| Thread | What it adds |
|---|---|
| Big Game | Better Combat move sets for the OreSpawn weapons (Big Bertha claymore presets and about 10 weapon attribute sets), Mobzilla-scale armor plating through Twilight Forest's traveller vest, three custom backpacks (Mobzilla hide, Kraken and Girlfriend), the Emperor's Chitin Band artifact, "Angel Insurance" (angels can pickpocket rubies, and Bertha hurts them), and decoy ore mines with a fair "spot the shimmer" tell |
| It Was Always Uranium | HBM's geiger counter also works as a dowsing rod for OreSpawn ore veins, a radioactive enchanting corner, a uranium collector chain with a hand-checked ProjectE EMC table, a uranium arc reactor, drinkable Bottled Uranium (the "Uranium Rush" buff), and uranium-belt A-10 ammo for the helicopters |
| The Royal Court | A boss-materials economy: Witherite-tempered Royal Guardian gear, spell scrolls tied to each boss's school (Godzilla for Fire, Kraken for Ice, King for Lightning, Queen for Holy), a Royal Dragon Egg you can hatch, pre-bound boss soul shards, scale upgrade orbs, and boss-loot uncrafting as the way items leave the economy |
| Her Side of the Story | A full story arc for the Girlfriend: her diary, the Date Night gift chain (flowers and foods from other mods that unlock diary pages and buffs), the Tome of the Girlfriend, and a Girlfriend familiar you can summon |
| The World Remembers | Summoning rites that depend on the weather (the Rite of Gojira needs a thunderstorm), a Mobzilla statue built through a catalyst grind, Crystalline world transmutation gated by EMC, and OreSpawn Rail Works, including survival recipes for the helicopters, which the MCHeli port never had |

Each thread has its own advancement tree, written in plain JSON, that also works as documentation. There is also an in-game Patchouli guidebook, and its recipes and IDs are checked against the actual pack jars.

Since 0.9.0, with [Monster Hunter Villager](#monster-hunter-villager-port) installed, Monster Hunters also hunt OreSpawn's rats, scorpions, leaf monsters, cave fishers and creeping horrors. The big predators are still the player's job.

It also carries the pack's compatibility mixins:

- A fix for a YUNG's API crash during structure generation, which happens when another mod creates a `Beardifier` early and one of its fields is left uninitialized.
- A fix for the Portal Gun's second portal, which is hard-coded to the physical left click. The mixin makes it use your attack keybinding instead.

## OreSpawn Delight

- Jar: `orespawn_delight-0.1.0.jar`
- Source: [CoolFreeze23/orespawn-delight](https://github.com/CoolFreeze23/orespawn-delight)

A [Farmer's Delight](https://modrinth.com/mod/farmers-delight) add-on for OreSpawn that turns the pack's creatures into ingredients: meats, cutting board drops, skillet dishes and cooking pot feasts from CrazyCraft mobs, made to look and progress like Farmer's Delight.

## Domestication Innovation port

- Jar: `domesticationinnovation-2.0.0-1.21.1.jar`
- Source: [CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1)
- Releases: [CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1/releases](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1/releases)

alex_the_668's pet overhaul never made it past Forge 1.20.1, so it was ported to NeoForge 1.21.1 and then extended. Pets get wander, stay and follow commands, enchantable collar tags, pet beds that bring them back to life, and a Wayward Lantern that fetches strays. Axolotls, foxes, rabbits and frogs can be tamed.

What this pack's build adds on top of the original:

- Nine new collar enchantments: Sonic Boom, Violent, Chaos (attackers end up Drunk and swing at each other), Paralysis, Share, Tough, Insight, XP Transfer and Night Vision.
- Datapacks can make any mob tameable, or convertible into another mob, with a single JSON file. Ocelots are tameable by default, and the pack can extend this to OreSpawn's own creatures without code.
- Animal Tamers sell the pet enchantment books at every trade tier, and librarians no longer have them in their trades.
- Jade integration, collars that drop when a pet dies for good, injured pets that back off instead of dying, roaming tied to the pet's bed, and an in-game config screen. All of it is translated to pt-BR.
- A long list of fixes against the 1.20.1 original, for behavior and for crashes, including one that kept the mod from running on dedicated servers at all. They're all in the repo's `CHANGELOG.md`, and a headless GameTest suite of 19 tests covers them.

## Monster Hunter Villager port

- Jar: `monster_hunter_villager-neoforge-1.21.1-1.3.1.jar`
- Source: [CoolFreeze23/monster-hunter-villager-neoforge](https://github.com/CoolFreeze23/monster-hunter-villager-neoforge)
- Releases: [CoolFreeze23/monster-hunter-villager-neoforge/releases](https://github.com/CoolFreeze23/monster-hunter-villager-neoforge/releases)

Our NeoForge 1.21.1 port of Yoshi's Monster Hunter Villager 1.2.1, which was made for Forge 1.20.1. The original ran its hunter and trap code on every mob in the world, every tick. On a test server with 500 zombies that added 1.5 to 2.4 ms to every tick. The port only runs that code for Monster Hunters and placed traps, and adds nothing measurable.

What you get in game:

- The Monster Hunter, a new villager profession with the Hunter's Workbench as its job site. Monster Hunters go after monsters within 15 blocks with thrown traps and the Hunter's Knife.
- Three traps: sticky, sharpened and soul chain. You can throw them too.
- The Hunter's Workbench, which crafts traps from a Trap Prototype.
- Hunter tents that generate in plains, forests, taigas, savannas, meadows and snowy plains.
- Trades in monster loot and hunting gear.

It comes in English and Brazilian Portuguese. Version 1.3.1 added the `monster_hunter_villager:quarry` entity tag, so other mods and datapacks can give the hunters more to hunt. [OreSpawn Integrations](#orespawn-integrations) uses it to add OreSpawn creatures.
