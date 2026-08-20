"""Generate docs/MODLIST.md from docs/mod_inventory.json."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
inv = json.loads((ROOT / "docs" / "mod_inventory.json").read_text(encoding="utf-8"))

NOTES = {
    "orespawn": "**Custom** — our NeoForge port of the 1.7.10 original ([repo](https://github.com/CoolFreeze23/Orespawn))",
    "orespawn_delight": "**Custom** — made for this pack ([repo](https://github.com/CoolFreeze23/orespawn-delight))",
    "orespawn_integrations": "**Custom** — made for this pack ([repo](https://github.com/CoolFreeze23/orespawn-integrations))",
    "securitycraft": "**Patched** — camera ChunkMap mixin removed (Immersive Portals conflict)",
    "ars_nouveau": "**Patched** — embedded lambdynamiclights-api stripped",
    "farmersrespite": "**Patched** — duplicate kettle menu registration removed (fixes a crash present in the published jar)",
    "mr_randomizer_completeedition": "**Patched** — crafting table recipe syntax fixed",
    "mob_mutator": "**Patched** — title-screen button mixin removed",
    "fancymenu": "**Patched** — allows the custom title screen to hide the copyright line",
    "yungsapi": "Updated to 5.1.7 for a worldgen NPE fix",
    "fabric_api": "Forgified Fabric API — pinned as a matched pair with Connector",
    "hbmsntm": "Unofficial 1.21.1 build of HBM's Nuclear Tech",
    "mcheli": "Unofficial 1.21.1 port of MC Helicopter",
}

FIXED = {
    "connector-2.0.0-beta.16+1.21.1-full.jar": ("connector", "Sinytra Connector", "2.0.0-beta.16", "loader tech", "Runs Fabric mods on NeoForge — pinned as a matched pair with Forgified Fabric API"),
    "dungeons-and-taverns-v4.4.4.jar": ("dungeons_and_taverns", "Dungeons and Taverns", "4.4.4", "datapack mod", ""),
    "kotlinforforge-5.8.0-all.jar": ("kotlinforforge", "Kotlin for Forge", "5.8.0", "neoforge", ""),
}

rows = []
for i in inv:
    if i["file"] in FIXED:
        modid, name, version, loader, note = FIXED[i["file"]]
    else:
        modid = i["modid"]
        name = i.get("name", modid)
        version = i["version"]
        loader = "Fabric (Connector)" if i["loader"].startswith("fabric") else "NeoForge"
        note = NOTES.get(modid, "")
    rows.append((name, modid, str(version), loader, note, i["file"]))

rows.sort(key=lambda r: r[0].lower())

out = ["# Full mod list", "",
       f"{len(rows)} mods. Minecraft 1.21.1 / NeoForge 21.1.248. "
       "Fabric-loader mods run through Sinytra Connector.", "",
       "| Mod | Version | Loader | Notes |",
       "|---|---|---|---|"]
for name, modid, version, loader, note, file in rows:
    out.append(f"| {name} (`{modid}`) | {version} | {loader} | {note} |")
out.append("")

(ROOT / "docs" / "MODLIST.md").write_text("\n".join(out), encoding="utf-8")
print(f"wrote MODLIST.md with {len(rows)} rows")
