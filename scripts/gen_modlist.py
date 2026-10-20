"""Generate docs/MODLIST.md from docs/mod_inventory.json."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
inv = json.loads((ROOT / "docs" / "mod_inventory.json").read_text(encoding="utf-8"))

NOTES = {
    "orespawn": "Custom: our NeoForge port of the 1.7.10 original ([repo](https://github.com/CoolFreeze23/Orespawn))",
    "orespawn_delight": "Custom: made for this pack ([repo](https://github.com/CoolFreeze23/orespawn-delight))",
    "orespawn_integrations": "Custom: made for this pack ([repo](https://github.com/CoolFreeze23/orespawn-integrations))",
    "domesticationinnovation": "Custom: our NeoForge port of alex_the_668's mod ([repo](https://github.com/CoolFreeze23/DomesticationInnovation-NeoForge-1.21.1))",
    "originawakening": "Custom: made for this pack ([repo](https://github.com/CoolFreeze23/originawakening))",
    "plantz": "Custom: our NeoForge 1.21.1 port of joshxviii's Plants & Zombies ([repo](https://github.com/CoolFreeze23/plants-and-zombies-neoforge))",
    "vramleakfixes": "Custom: made for this pack ([repo](https://github.com/CoolFreeze23/vramleakfixes))",
    "monster_hunter_villager": "Custom: our NeoForge port of Yoshi's mod ([repo](https://github.com/CoolFreeze23/monster-hunter-villager-neoforge))",
    "securitycraft": "Patched: camera ChunkMap mixin removed (Immersive Portals conflict)",
    "ars_nouveau": "Patched: embedded lambdynamiclights-api stripped",
    "farmersrespite": "Patched: duplicate kettle menu registration removed (the published jar crashes without this)",
    "mr_randomizer_completeedition": "Patched: crafting table recipe syntax fixed",
    "mob_mutator": "Patched: title screen button mixin removed",
    "fancymenu": "Patched: lets the custom title screen hide the copyright line",
    "yungsapi": "Updated to 5.1.7 for a worldgen NPE fix",
    "fabric_api": "Fabric API for the mods Connector runs; must stay at 0.116.7 or newer for Soul Shards Despawn",
    "connector": "Runs Fabric mods on NeoForge",
    "hbmsntm": "Unofficial 1.21.1 build of HBM's Nuclear Tech",
    "mcheli": "Unofficial 1.21.1 port of MC Helicopter",
}

# keyed by mod id, not file name, so they survive updates
LOADER_LABELS = {"connector": "loader tech"}

rows = []
for i in inv:
    modid = i["modid"]
    name = i.get("name", modid)
    version = i["version"]
    if modid in LOADER_LABELS:
        loader = LOADER_LABELS[modid]
    elif i["loader"] == "datapack":
        loader = "datapack mod"
    else:
        loader = "Fabric (Connector)" if i["loader"].startswith("fabric") else "NeoForge"
    note = NOTES.get(modid, "")
    rows.append((name, modid, str(version), loader, note, i["file"]))

rows.sort(key=lambda r: r[0].lower())

out = ["# Full mod list", "",
       f"{len(rows)} mods. Minecraft 1.21.1 / NeoForge 21.1.252. "
       "Fabric-loader mods run through Sinytra Connector.", "",
       "| Mod | Version | Loader | Notes |",
       "|---|---|---|---|"]
for name, modid, version, loader, note, file in rows:
    out.append(f"| {name} (`{modid}`) | {version} | {loader} | {note} |")
out.append("")

(ROOT / "docs" / "MODLIST.md").write_text("\n".join(out), encoding="utf-8")
print(f"wrote MODLIST.md with {len(rows)} rows")
