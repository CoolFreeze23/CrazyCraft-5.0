"""Build CrazyCraft5-Client.zip and the server pack directory.

Client zip = MultiMC/Prism importable instance.
Server dir = mods (minus client-only) + configs + start scripts; zipped after boot testing.
"""
import json
import shutil
import sys
import zipfile
from pathlib import Path

INSTANCE = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\instances\CrazyCraft 5.0")
MC = INSTANCE / "minecraft"
ROOT = Path(__file__).resolve().parent.parent
BUILD = ROOT / "build"
NEOFORGE_VERSION = "21.1.248"

# Client-only mods excluded from the dedicated server (filename match, case-insensitive).
# Fabric jars that declare "environment": "client", plus NeoForge mods that are
# client-side by nature (rendering/HUD/menu/audio/particles).
SERVER_EXCLUDE = [
    # fabric, declared client
    "BiomeParticleWeather", "capes-", "cosy-critters", "crumbling-hearts",
    "explosive-enhancement", "visuality-", "wakes-",
    # rendering / performance client stack
    "sodium-neoforge", "sodiumdynamiclights", "iris-neoforge", "entityculling",
    "continuity-",
    # HUD / UI / menu
    "fancymenu", "melody_neoforge", "draggable_lists", "fancytoasts",
    "autohud", "BetterAdvancements", "Controlling-", "MouseTweaks",
    "invtweaks", "Neat-", "shouldersurfing", "ShoulderSurfing",
    # audio / camera / cosmetics
    "AmbientSounds", "CameraOverhaul", "Fog-neoforge", "waveycapes",
    "notenoughanimations", "seriousplayeranimations", "golem_spawn_animation",
    # particles / visual effects
    "Pretty Rain", "particular-", "SubtleEffects", "eg_particle_interactions",
    "Perception-NEOFORGE", "hold-my-items", "HMI ",
    # client-side helpers
    "eating-animation",
    # held-item and entity rendering (client only)
    "punchy-", "entity_model_features", "entity_texture_features",
    # NOTE: atlas-core must stay - Pandora's Box hard-requires it on the server.
]

CLIENT_DIRS = ["mods", "config", "defaultconfigs", "resourcepacks", "mcheli",
               "moonlight-global-datapacks", "patchouli_books"]
CLIENT_FILES = ["emi.json", "patchouli_data.json", "icon.png"]

OPTIONS_TXT = "version:3955\nlang:pt_br\nresourcePacks:[\"mod/punchy:resourcepacks/punchy\",\"vanilla\",\"fabric\",\"mod_resources\",\"moonlight:merged_pack\",\"file/Fast Better Grass.zip\",\"file/Better Leaves.zip\",\"file/Low On Fire.zip\",\"file/CrazyCraft5-ptBR.zip\",\"file/Drigo 3D Lanterns x Punchy.zip\",\"file/Traben\\u0027s 3D Armor - 1.0.1.zip\",\"file/Untitled Punchy.zip\",\"file/Sun and Moon Circular.zip\",\"file/trabens-3d-arrows-1.1.zip\",\"file/Hyper Punchy.zip\",\"file/Fresh Food.zip\",\"file/Even Better Enchants.zip\",\"file/Enhanced Boss Bars.zip\",\"file/Dramatic Skys.zip\",\"file/Blockier Goat Horn v1.1 f9-34.zip\",\"file/Actually 3D Stuff.zip\",\"file/FreshAnimations_v1.9.2.zip\",\"file/FA+Emissive-v1.2.zip\",\"file/Alittle_Axolotl.zip\"]\n"

INSTANCE_CFG = """[General]
ConfigVersion=1.2
InstanceType=OneSix
iconKey=default
name=CrazyCraft 5.0
OverrideMemory=true
MinMemAlloc=2048
MaxMemAlloc=8192
"""


def iter_client_files():
    """Yield (source path, archive path inside .minecraft) for the client pack."""
    for d in CLIENT_DIRS:
        base = MC / d
        if not base.is_dir():
            continue
        for p in base.rglob("*"):
            if not p.is_file():
                continue
            rel = p.relative_to(MC).as_posix()
            # skip disabled mods and the unzipped resource pack folder (dupe of the zip)
            if d == "mods" and not p.name.endswith(".jar"):
                continue
            if rel.startswith("resourcepacks/CrazyCraft-PTBR/"):
                continue
            yield p, rel
    for f in CLIENT_FILES:
        p = MC / f
        if p.is_file():
            yield p, f


def build_client():
    out = BUILD / "CrazyCraft5-Client.zip"
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists():
        out.unlink()
    n = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, strict_timestamps=False) as z:
        z.writestr("instance.cfg", INSTANCE_CFG)
        z.write(INSTANCE / "mmc-pack.json", "mmc-pack.json")
        z.writestr(".minecraft/options.txt", OPTIONS_TXT)
        for src, rel in iter_client_files():
            z.write(src, f".minecraft/{rel}")
            n += 1
    print(f"client: {n} files -> {out} ({out.stat().st_size/1048576:.0f} MB)")


def is_excluded(name):
    low = name.lower()
    return any(pat.lower() in low for pat in SERVER_EXCLUDE)


def build_server_dir():
    sv = BUILD / "server-pack"
    if sv.exists():
        shutil.rmtree(sv)
    sv.mkdir(parents=True)

    kept, skipped = [], []
    (sv / "mods").mkdir()
    for p in sorted((MC / "mods").glob("*.jar")):
        if is_excluded(p.name):
            skipped.append(p.name)
        else:
            shutil.copy2(p, sv / "mods" / p.name)
            kept.append(p.name)

    for d in ["config", "defaultconfigs", "mcheli", "moonlight-global-datapacks"]:
        src = MC / d
        if src.is_dir():
            shutil.copytree(src, sv / d)

    if (MC / "icon.png").is_file():
        shutil.copy2(MC / "icon.png", sv / "server-icon.png")

    for f in (ROOT / "server").iterdir():
        shutil.copy2(f, sv / f.name)

    (sv / "REMOVED_CLIENT_MODS.txt").write_text("\n".join(skipped), encoding="utf-8")
    print(f"server: kept {len(kept)} mods, removed {len(skipped)} client-only:")
    for s in skipped:
        print("  -", s)


def zip_server():
    sv = BUILD / "server-pack"
    out = BUILD / "CrazyCraft5-Server.zip"
    if out.exists():
        out.unlink()
    n = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, strict_timestamps=False) as z:
        for p in sv.rglob("*"):
            if p.is_file():
                # runtime artifacts from boot testing must not ship
                rel = p.relative_to(sv).as_posix()
                if rel.split("/")[0] in ("libraries", "world", "logs", "crash-reports",
                                         "cache", "debug", ".cache", ".minecraft",
                                         ".mixin.out", ".connector", "journeymap",
                                         "local", "dynamic-data-pack-cache",
                                         "customnpcs", "patchouli_books"):
                    continue
                if rel in ("eula.txt", "usercache.json", "usernamecache.json",
                           "session.lock", "banned-ips.json", "banned-players.json",
                           "ops.json", "whitelist.json", "boot-test.log",
                           "fabricloader.log", "run.bat", "run.sh",
                           "user_jvm_args.txt", "neoforge-21.1.248-installer.jar.log"):
                    continue
                z.write(p, rel)
                n += 1
    print(f"server zip: {n} files -> {out} ({out.stat().st_size/1048576:.0f} MB)")


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    if what in ("all", "client"):
        build_client()
    if what in ("all", "server"):
        build_server_dir()
    if what == "zipserver":
        zip_server()
