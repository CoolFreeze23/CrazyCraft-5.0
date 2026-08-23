# The custom mods

Four mods in this pack are maintained in-house: three built specifically for it, plus a rebuilt Domestication Innovation. All four are open source under the same GitHub account, and each ships as a normal jar in `mods/`.

---

## OreSpawn 2.0 — the port

**Jar:** `orespawn-1.21.1-2.0.0-beta.x.jar` · **Source:** [CoolFreeze23/Orespawn](https://github.com/CoolFreeze23/Orespawn) · **Releases:** [here](https://github.com/CoolFreeze23/Orespawn/releases)

The centerpiece of the pack. The original OreSpawn only ever existed for Minecraft 1.7.10, so it was ported to NeoForge 1.21.1 **from the original source code**, with the explicit goal of *faithful parity* — no reinvented content, no "modernized" balance, no invented behavior.

How it was done (all of this is documented inside the repo itself):

- The original 1.7.10 source lives in the repo under `reference_1_7_10_source/` and every ported system cites the original file and line it came from.
- A full audit of the original produced **605 tracked findings** (`AUDIT_FINDINGS.md`), each with a stable ID, worked through in phases (`IMPLEMENTATION_PLAN.md`, phase reports in `phase_d_reports/`).
- Every fix and decision is logged in `FIX_LOG.md`; deliberate deviations (there are very few) are recorded and signed off in `PARITY_NOTES`.
- The ~25 hardcoded 1.7.10 structures were mapped to modern worldgen mechanisms following a documented conversion pattern, and the ~105-type SpawnOres block pool and 116 water-bucket egg recipes were reproduced exactly.

What you get in game: the Crystal, Mining, Village, and Ant dimensions, the full boss ladder (Mobzilla, the King, the Queen, the Kraken, the Emperor Scorpion, the Basilisc...), Girlfriends and Boyfriends, Big Bertha and the Royal Guardian set, uranium/titanium ore progression, ant nests, dungeons — the whole 1.7.10 experience.

Notable parity fixes shipped during this pack's development (see the repo's `CHANGELOG.md`):

- **Ant & termite overhaul** — the port had invented natural biome spawning for ants that the original never had; it was removed. Original despawn behavior, nest spawn caps, the termites' replicate-on-eat behavior, biome-tinted anthill blocks, and the original 0.25×/0.35× render scales were all restored.
- **Mining dimension daylight** — the dimension was wrongly declared as a dark, ceilinged cavern; restored to the original's open-sky daylight surface world.
- **Worldgen crash fix** — OreSpawn's lake feature could crash chunk generation when Serene Seasons redirected the vanilla freeze check into an out-of-region biome lookup; the freeze logic is now inlined region-safe.

## OreSpawn Integrations

**Jar:** `orespawn_integrations-0.4.x.jar` · **Source:** [CoolFreeze23/orespawn-integrations](https://github.com/CoolFreeze23/orespawn-integrations)

A companion mod written for this pack that makes OreSpawn feel *native* to the other 190 mods instead of bolted on. Design rule: **zero hard dependencies** — every integration checks whether the target mod is loaded, so removing any mod breaks nothing, and every thread can be toggled in config.

Content is organized as five named threads:

| Thread | What it adds |
|---|---|
| **Big Game** | Better Combat move-sets for the OreSpawn arsenal (Big Bertha claymore presets and ~10 weapon attribute sets), Mobzilla-scale armor plating via Twilight Forest's traveller vest, three custom backpacks (Mobzilla-hide / Kraken / Girlfriend), the Emperor's Chitin Band artifact, "Angel Insurance" (angels can pickpocket rubies — and Bertha hurts them), and decoy ore mines with a fair "spot the shimmer" tell |
| **It Was Always Uranium** | HBM's geiger counter doubles as an ore-dowsing rod for OreSpawn veins, a radioactive enchanting corner, a uranium collector chain with a hand-reviewed ProjectE EMC table, a uranium arc reactor, drinkable Bottled Uranium (the "Uranium Rush" buff), and uranium-belt A-10 ammo for the helicopters |
| **The Royal Court** | A boss-materials economy: Witherite-tempered Royal Guardian gear, boss-school spell scrolls (Godzilla=Fire, Kraken=Ice, King=Lightning, Queen=Holy), a hatchable Royal Dragon Egg, pre-bound boss soul shards, scale upgrade orbs, and boss-loot uncrafting as the economy's sink |
| **Her Side of the Story** | A full narrative arc for the Girlfriend: her diary, the Date Night gift chain (cross-mod flowers and foods that unlock diary pages and buffs), the Tome of the Girlfriend, and a summonable Girlfriend familiar |
| **The World Remembers** | Weather-gated summoning rites (the Rite of Gojira needs a thunderstorm), a Mobzilla statue built through a catalyst grind, EMC-gated Crystalline world transmutation, and OreSpawn Rail Works — including survival recipes for the helicopters, which the MCHeli port never had |

Each thread ships a pure-JSON advancement tree that doubles as documentation, and the whole thing is documented in-game by a **Patchouli guidebook** whose recipes and IDs are validated against the actual pack jars.

It also carries the pack's compatibility mixins:

- **Beardifier init fix** — guards against a YUNG's API crash during structure generation (uninitialized field when other mods construct a `Beardifier` early).
- **Portal Gun keybind fix** — the Portal Gun hard-codes physical left-click for its second portal; the mixin makes it respect your actual attack keybinding.

## OreSpawn Delight

**Jar:** `orespawn_delight-0.1.0.jar` · **Source:** [CoolFreeze23/orespawn-delight](https://github.com/CoolFreeze23/orespawn-delight)

A [Farmer's Delight](https://modrinth.com/mod/farmers-delight) kitchen expansion for OreSpawn: the pack's creatures become ingredients. Meats, cutting-board drops, skillet dishes, and cooking-pot feasts sourced from CrazyCraft fauna, in Farmer's Delight's visual and progression style.

## Domestication Innovation — the port

**Jar:** `domesticationinnovation-2.0.0-1.21.1.jar` · **Source:** [CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1) · **Releases:** [here](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1/releases)

alex_the_668's pet-overhaul mod never left Forge 1.20.1, so it was ported to NeoForge 1.21.1 and then taken further. Pets get a wander/stay/follow command system, enchantable collar tags, pet beds that resurrect them, and a Wayward Lantern that fetches strays; axolotls, foxes, rabbits and frogs become tameable.

What this pack's build adds on top of the original:

- **Nine new collar enchantments** — Sonic Boom, Violent, Chaos (which leaves attackers Drunk and swinging at each other), Paralysis, Share, Tough, Insight, XP Transfer and Night Vision.
- **Datapack-driven taming** — any mob can be made tameable, or convertible into another mob, with a single JSON file. Ocelots ship tameable by default; the pack can extend this to OreSpawn's own creatures without code.
- **Animal Tamers sell the pet enchantment books** at every trade tier, and librarians no longer clutter their pools with them.
- **Jade integration**, collars that drop when a pet dies for good, injured pets that retreat instead of dying, bed-anchored roaming, and an in-game config screen — all fully translated to pt-BR.
- A long list of parity and crash fixes against the 1.20.1 original, including one that stopped the mod from running on dedicated servers at all. The full record is in the repo's `CHANGELOG.md`, and a 19-test headless GameTest suite guards it.
