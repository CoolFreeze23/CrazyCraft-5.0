"""Build CrazyCraft5-Client.zip and the server pack directory.

Client zip = MultiMC/Prism importable instance.
Server files = the loader, its manifest, start scripts and configs; no mod jars (pack/manifest.json
says where each mod is downloaded from). "assemble" installs a complete server with the loader.
"""
import json
import copy
import shutil
import sys
import zipfile
from pathlib import Path

INSTANCE = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\instances\CrazyCraft 5.0")
MC = INSTANCE / "minecraft"
ROOT = Path(__file__).resolve().parent.parent
BUILD = ROOT / "build"
NEOFORGE_VERSION = "21.1.252"

# Client-only mods excluded from the dedicated server (filename match, case-insensitive).
# Fabric jars that declare "environment": "client", plus NeoForge mods that are
# client-side by nature (rendering/HUD/menu/audio/particles).
SERVER_EXCLUDE = [
    # fabric, declared client
    "BiomeParticleWeather", "capes-", "cosy-critters", "crumbling-hearts",
    "explosive-enhancement", "visuality-", "wakes-",
    # rendering / performance client stack
    "sodium-neoforge", "sodiumdynamiclights", "iris-neoforge", "entityculling",
    "continuity-", "ImmediatelyFast",
    # HUD / UI / menu
    "fancymenu", "melody_neoforge", "draggable_lists", "fancytoasts",
    "autohud", "BetterAdvancements", "Controlling-", "MouseTweaks",
    # invtweaks (Inventory Tweaks Refoxed) is NOT client-only: its channels packet_sort_inv / packet_update_config are
    # required by the client, so the server must carry it (v1.2.0 join failure, 2026-09-20)
    "Neat-", "shouldersurfing", "ShoulderSurfing",
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
    # discord rich presence (client only; CraterLib is only here for Simple RPC)
    "SimpleRPC", "CraterLib",
    # NOTE: atlas-core must stay - Pandora's Box hard-requires it on the server.,
    "cosycritters", "sodiumoptionsapi", "modefite",
    "txfastload",
    # our client-side mods: Origin Awakening (origin picker and looks) and VRAM Leak Fixes
    "originawakening", "vramleakfixes",
]

CLIENT_DIRS = ["mods", "config", "defaultconfigs", "resourcepacks", "mcheli",
               "moonlight-global-datapacks", "patchouli_books"]
CLIENT_FILES = ["emi.json", "patchouli_data.json", "icon.png"]

OPTIONS_TXT = "version:3955\nlang:pt_br\nresourcePacks:[\"mod/punchy:resourcepacks/punchy\",\"vanilla\",\"fabric\",\"mod_resources\",\"moonlight:merged_pack\",\"file/Fast Better Grass.zip\",\"file/Better-Leaves-9.6.zip\",\"file/Low On Fire.zip\",\"file/CrazyCraft5-ptBR.zip\",\"file/Drigo 3D Lanterns x Punchy.zip\",\"file/Traben\\u0027s 3D Armor - 1.0.1.zip\",\"file/Untitled Punchy.zip\",\"file/Sun and Circular Moon.zip\",\"file/trabens-3d-arrows-1.1.zip\",\"file/Hyper Punchy.zip\",\"file/Fresh Food.zip\",\"file/Even Better Enchants.zip\",\"file/Enhanced Boss Bars.zip\",\"file/Dramatic Skys Demo 1.5.3.36.6.zip\",\"file/Blockier Goat Horn v1.2.zip\",\"file/FreshAnimations_v1.10.4.zip\",\"file/FA+Emissive-v1.6.zip\",\"file/Alittle_Axolotl.zip\"]\n"

INSTANCE_CFG = """[General]
ConfigVersion=1.2
InstanceType=OneSix
iconKey=default
name=CrazyCraft 5.0
OverrideMemory=true
MinMemAlloc=2048
MaxMemAlloc=8192
OverrideCommands=true
PreLaunchCommand="\\"$INST_JAVA\\" -jar \\"$INST_MC_DIR/crazycraft-loader.jar\\" --client"
PostExitCommand=
WrapperCommand=
"""


def iter_client_files():
    """Yield (source path, archive path inside .minecraft) for the client pack.

    No mods and no third-party resource packs: the loader downloads those on the first launch (pack/manifest.json).
    Only the pack's own resource packs ship, and files the loader copies out of a mod's jar are left out too.
    """
    sources = json.loads((ROOT / "pack" / "sources.json").read_text(encoding="utf-8"))
    bundled = set(sources.get("resourcepacks", {}).get("bundled", []))
    extracted = {x["to"] for x in load_manifest()["extract"]}
    for d in CLIENT_DIRS:
        if d == "mods":
            continue
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
            if d == "resourcepacks" and p.name not in bundled:
                continue
            if rel in extracted:
                continue
            # NeoForge's copies of replaced config files (name-N.toml.bak); nothing reads them
            if p.name.endswith(".toml.bak"):
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
        z.write(BUILD / "loader" / "crazycraft-loader.jar", ".minecraft/crazycraft-loader.jar")
        z.write(ROOT / "pack" / "manifest.json", ".minecraft/crazycraft-manifest.json")
        credits = BUILD / "CREDITS-client.txt"
        write_credits(load_manifest(), credits, client=True)
        z.write(credits, "CREDITS.txt")
        n += 3
        for src, rel in iter_client_files():
            z.write(src, f".minecraft/{rel}")
            n += 1
    print(f"client: {n} files -> {out} ({out.stat().st_size/1048576:.0f} MB)")


def is_personal(name):
    """Jars kept in my own instance only (pack/sources.json "personal"): never in the manifest, the mod list or a build."""
    import fnmatch
    import json as _json
    spec = _json.loads((ROOT / "pack" / "sources.json").read_text(encoding="utf-8")).get("personal", {})
    return any(fnmatch.fnmatch(name, pat) for pat in spec)


def is_excluded(name):
    low = name.lower()
    return any(pat.lower() in low for pat in SERVER_EXCLUDE)


def load_manifest():
    return json.loads((ROOT / "pack" / "manifest.json").read_text(encoding="utf-8"))


def write_credits(manifest, path, client=False):
    what = "mods and resource packs" if client else "mods"
    lines = [f"{manifest['pack']['name']} {manifest['pack']['version']}: where the {what} come from",
             "=" * 72, "",
             f"The pack doesn't host other people's {what}. The setup downloads each one from the page",
             "below, where you also find its license and support. Thanks to every author in this list.", ""]
    mods = sorted((m for m in manifest["mods"] if m["side"] != ("server" if client else "client")),
                  key=lambda m: (m.get("dir", "mods") != "mods", m["name"].lower()))
    for m in mods:
        src = {"modrinth": "Modrinth", "curseforge": "CurseForge", "github": "GitHub",
               "manual": "CurseForge (download by hand)"}[m["source"]["type"]]
        lic = f", license {m['license']}" if m.get("license") and m["license"] != "See repository" else ""
        lines.append(f"{m['name']} {m['version']}  ({src}{lic})")
        lines.append(f"    {m['source']['page']}")
        if m.get("patch"):
            lines.append(f"    Fixed for the pack on install: {m['patch']['why']}")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def build_server_files():
    """The server files players download: the loader, its manifest, start scripts and configs. No mod jars."""
    out = BUILD / "server-files"
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    manifest = load_manifest()
    loader = BUILD / "loader" / "crazycraft-loader.jar"
    if not loader.is_file():
        raise SystemExit("build the loader first: python loader/build.py")
    shutil.copy2(loader, out / "crazycraft-loader.jar")
    shutil.copy2(ROOT / "pack" / "manifest.json", out / "crazycraft-manifest.json")
    for d in ["config", "defaultconfigs", "mcheli", "moonlight-global-datapacks"]:
        src = MC / d
        if src.is_dir():
            shutil.copytree(src, out / d, ignore=shutil.ignore_patterns("*.toml.bak"))
    # files the loader copies out of a mod's own jar on install don't ship with the pack
    for x in manifest["extract"]:
        p = out / x["to"]
        if p.is_file():
            p.unlink()
    if (MC / "icon.png").is_file():
        # Minecraft only accepts a 64x64 server-icon.png; the instance icon is 192x192.
        try:
            from PIL import Image
            Image.open(MC / "icon.png").convert("RGBA").resize((64, 64), Image.LANCZOS).save(out / "server-icon.png")
        except ImportError:
            print("  (Pillow missing: server-icon.png skipped, it must be 64x64)")
    for f in (ROOT / "server").iterdir():
        if f.is_file() and f.suffix != ".jar":
            data = f.read_bytes().replace(b"\r\n", b"\n")
            if f.suffix == ".bat":
                data = data.replace(b"\n", b"\r\n")
            (out / f.name).write_bytes(data)
    write_credits(manifest, out / "CREDITS.txt")
    jars = sum(1 for _ in out.rglob("*.jar"))
    server_mods = sum(1 for m in manifest["mods"] if m["side"] != "client")
    print(f"server files: {sum(1 for p in out.rglob('*') if p.is_file())} files, {jars} jar (the loader), "
          f"{server_mods} mods in the manifest -> {out}")


def zip_server():
    src = BUILD / "server-files"
    out = BUILD / "CrazyCraft5-Server.zip"
    if out.exists():
        out.unlink()
    n = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, strict_timestamps=False) as z:
        for p in sorted(src.rglob("*")):
            if p.is_file():
                info = zipfile.ZipInfo.from_file(p, p.relative_to(src).as_posix(), strict_timestamps=False)
                if p.suffix == ".sh":
                    info.external_attr = 0o100755 << 16
                info.compress_type = zipfile.ZIP_DEFLATED
                z.writestr(info, p.read_bytes())
                n += 1
    print(f"server zip: {n} files -> {out} ({out.stat().st_size / 1048576:.1f} MB)")


def build_setup(server_zip=None):
    """Combined launcher/client and server setup; client files come from the latest public release."""
    out = BUILD / "CrazyCraft5-Setup.zip"
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, strict_timestamps=False) as z:
        if server_zip:
            with zipfile.ZipFile(server_zip) as server:
                for entry in server.infolist():
                    z.writestr(copy.copy(entry), server.read(entry))
        else:
            for p in sorted((BUILD / "server-files").rglob("*")):
                if p.is_file():
                    z.write(p, p.relative_to(BUILD / "server-files").as_posix())
        bat = (ROOT / "server" / "CrazyCraft Server Setup.bat").read_text(encoding="utf-8")
        z.writestr("CrazyCraft Setup.bat", bat.replace("--server-setup", "--setup").replace("\n", "\r\n"))
        portuguese = bat.replace("--server-setup", "--setup --lang pt_br")
        portuguese = portuguese.replace("@echo off", "@echo off\nchcp 65001 >nul")
        portuguese = portuguese.replace("CrazyCraft 5.0 needs Java 21, and Java was not found.", "O CrazyCraft 5.0 precisa do Java 21, mas o Java não foi encontrado.")
        portuguese = portuguese.replace("Get it from", "Baixe em")
        z.writestr("Instalar CrazyCraft (PT-BR).bat", portuguese.replace("\n", "\r\n").encode("utf-8"))
        info = zipfile.ZipInfo("setup.sh")
        info.external_attr = 0o100755 << 16
        z.writestr(info, '#!/bin/sh\ncd "$(dirname "$0")" || exit 1\nexec "${CRAZYCRAFT_JAVA:-java}" -jar crazycraft-loader.jar --setup\n')
        z.writestr("START HERE.txt", "CrazyCraft Setup\n\nRequires Java 21. Open your launcher and sign in once, then close it.\n"
                   "Windows: double-click CrazyCraft Setup.bat. Linux/macOS: run ./setup.sh.\n"
                   "Choose Prism Launcher or Minecraft Launcher. Select an existing CrazyCraft installation to update,\n"
                   "or create a new one. Setup downloads the latest published release and its mods.\n"
                   "Portable launchers: choose their data folder using Browse.\n"
                   "Updates preserve saves, server lists and player options. Replaced files are backed up under\n"
                   ".crazycraft/backups in the game folder; obsolete pack mods move to mods-removed.\n"
                   "Close Minecraft and its launcher throughout setup. Rerun setup to resume an interrupted install.\n"
                   "For a dedicated server, choose Set up a server instead (see README.txt).\n")
        z.writestr("LEIA-ME.txt", "Instalação do CrazyCraft\n\nRequer Java 21. Abra seu launcher e entre na conta uma vez, depois feche-o.\n"
                   "No Windows, execute Instalar CrazyCraft (PT-BR).bat. No Linux/macOS, use:\n"
                   "java -jar crazycraft-loader.jar --setup --lang pt_br\n\n"
                   "Escolha Prism Launcher ou Minecraft Launcher e crie uma instalação ou atualize uma existente.\n"
                   "O idioma pode ser alterado no menu English / Português (Brasil) e a escolha fica salva.\n"
                   "Em sistemas em português, o instalador usa português automaticamente.\n"
                   "Mantenha o jogo e o launcher fechados até terminar. Ao concluir, abra o launcher e selecione a instalação.\n"
                   "Mundos e preferências são preservados. Os arquivos substituídos têm backup em .crazycraft/backups.\n"
                   "Mods antigos do pacote são movidos para mods-removed. Se houver interrupção, execute o instalador novamente.\n"
                   "O MCHeli exige download manual pelo CurseForge; o instalador orienta você.\n")
    print(f"combined setup: {out} ({out.stat().st_size / 1048576:.1f} MB)")


def build_installer_update(baseline):
    """Release only installer changes, preserving the previous release's game files byte for byte."""
    manifest = load_manifest()
    for name, prefix in (("CrazyCraft5-Client.zip", ".minecraft/"), ("CrazyCraft5-Server.zip", "")):
        source = baseline / name
        target = BUILD / name
        if source.resolve() == target.resolve():
            raise ValueError("Baseline must be separate from the build output")
        with zipfile.ZipFile(source) as old:
            previous = json.loads(old.read(prefix + "crazycraft-manifest.json"))
            previous_version = previous["pack"]["version"]
            previous["pack"]["version"] = manifest["pack"]["version"]
            if previous != manifest:
                raise ValueError("Installer-only releases cannot change the mod manifest")
            replacements = {
                prefix + "crazycraft-loader.jar": (BUILD / "loader/crazycraft-loader.jar").read_bytes(),
                prefix + "crazycraft-manifest.json": (ROOT / "pack/manifest.json").read_bytes(),
            }
            if not prefix:
                replacements["CrazyCraft Server Setup.bat"] = (ROOT / "server/CrazyCraft Server Setup.bat").read_bytes()
            with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as new:
                for entry in old.infolist():
                    data = replacements.get(entry.filename, old.read(entry))
                    if entry.filename == "CREDITS.txt":
                        data = data.replace(
                            f"CrazyCraft 5.0 {previous_version}".encode(),
                            f"CrazyCraft 5.0 {manifest['pack']['version']}".encode(), 1)
                    new.writestr(copy.copy(entry), data)
        print(f"installer update: {target}")
    build_setup(BUILD / "CrazyCraft5-Server.zip")


def assemble_server():
    """A complete server in build/server-pack, installed by the loader itself: for boot tests and the pack's server.

    The by-hand mods are copied in from the instance, as a player would save them into manual/. Our own mods whose
    GitHub release isn't published yet are placed in mods/ up front (the loader checks them like any other file).
    """
    import subprocess
    import urllib.error
    import urllib.request
    manifest = load_manifest()
    sv = BUILD / "server-pack"
    if sv.exists():
        shutil.rmtree(sv)
    shutil.copytree(BUILD / "server-files", sv)
    (sv / "manual").mkdir(exist_ok=True)
    (sv / "mods").mkdir(exist_ok=True)
    for m in manifest["mods"]:
        if m["side"] == "client":
            continue
        if m["source"]["type"] == "manual":
            shutil.copy2(MC / "mods" / m["file"], sv / "manual" / m["file"])
            print(f"  staged by hand: {m['file']}")
        elif m["source"]["type"] == "github":
            try:
                req = urllib.request.Request(m["source"]["url"], method="HEAD",
                                             headers={"User-Agent": "CoolFreeze23/CrazyCraft-5.0"})
                urllib.request.urlopen(req, timeout=30).close()
            except urllib.error.HTTPError:
                shutil.copy2(MC / "mods" / m["file"], sv / "mods" / m["file"])
                print(f"  not released yet, placed from the instance: {m['file']}")
    java = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\java\java-runtime-delta\bin\java.exe")
    r = subprocess.run([str(java), "-jar", str(sv / "crazycraft-loader.jar"), "--nogui", "--accept-eula",
                        "--dir", str(sv)], cwd=sv)
    print("loader exit:", r.returncode)
    if r.returncode != 0:
        raise SystemExit(r.returncode)


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    if what == "installer-update":
        build_installer_update(Path(sys.argv[2]))
    if what in ("all", "client"):
        build_client()
    if what in ("all", "server", "setup"):
        build_server_files()
        zip_server()
    if what in ("all", "setup"):
        build_setup()
    if what == "zipserver":
        zip_server()
    if what == "assemble":
        assemble_server()
