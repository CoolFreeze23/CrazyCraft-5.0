"""Scan the instance mods folder and emit a JSON inventory of every jar.

Reads neoforge.mods.toml / fabric.mod.json out of each jar to get mod id,
display name, version, loader, and (for fabric) declared environment.
Wrapper jars that only hold jar-in-jar files (Kotlin for Forge, MezzConfig,
Connector) are read from the nested jar that declares the mod, as NeoForge does.
"""
import io
import json
import re
import sys
import tomllib
import zipfile
from pathlib import Path

MODS_DIR = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\instances\CrazyCraft 5.0\minecraft\mods")
OUT = Path(__file__).resolve().parent.parent / "docs" / "mod_inventory.json"
MC_VERSION = "1.21.1"


def parse_toml_mods(text):
    """Cheap parse of the [[mods]] tables in a mods.toml."""
    mods = []
    current = None
    in_deps = False
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("[[mods]]"):
            current = {}
            mods.append(current)
            in_deps = False
            continue
        if s.startswith("[[dependencies") or s.startswith("[dependencies"):
            in_deps = True
            continue
        if s.startswith("[["):
            in_deps = True  # some other table
            continue
        if current is None or in_deps:
            continue
        m = re.match(r'(\w+)\s*=\s*"(.*?)"', s)
        if m:
            current[m.group(1)] = m.group(2)
    return mods


def toml_mods(text):
    """The mods a mods.toml declares. tomllib reads every layout, including the inline `mods = [{ ... }]`
    of Modrinth's datapack jars; the cheap parser covers files tomllib rejects."""
    try:
        mods = tomllib.loads(text).get("mods", [])
    except tomllib.TOMLDecodeError:
        return parse_toml_mods(text)
    return [m for m in mods if isinstance(m, dict)]


def manifest_version(z):
    """Implementation-Version from the jar's manifest, which is what ${file.jarVersion} stands for."""
    try:
        mf = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    except KeyError:
        return None
    mv = re.search(r"Implementation-Version:\s*(\S+)", mf)
    return mv.group(1) if mv else None


def file_name_version(name):
    """Last version-like part of the file name (CarbonConfig-Neoforge-1.21.1-2.0.2.1.jar -> 2.0.2.1), for jars
    whose manifest has no version; NeoForge shows those as 0.0NONE."""
    parts = [p for p in re.split(r"[-_]", Path(name).stem) if re.match(r"v?\d", p) and p != MC_VERSION]
    return parts[-1].lstrip("v") if parts else None


def read_neoforge(z, fallback_version):
    """Loader, mod id, name and version of the first mod in the jar's mods.toml; None if it declares none."""
    names = set(z.namelist())
    toml_name = next((c for c in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml") if c in names), None)
    if not toml_name:
        return None
    mods = toml_mods(z.read(toml_name).decode("utf-8", "replace"))
    if not mods:
        return None
    m = mods[0]
    v = str(m.get("version", "?"))
    if "${" in v:  # version substituted from the manifest
        v = manifest_version(z) or fallback_version or "?"
    modid = m.get("modId", "?")
    # Modrinth turns datapacks into mod jars; their mods.toml points back to the datapack page
    loader = "datapack" if "modrinth.com/datapack/" in str(m.get("displayURL", "")) else "neoforge"
    return {"loader": loader, "modid": modid, "name": m.get("displayName", modid), "version": v}


def read_nested(z):
    """The first jar-in-jar entry that declares a mod, versioned by its jar-in-jar entry if its manifest doesn't say."""
    try:
        meta = json.loads(z.read("META-INF/jarjar/metadata.json"))
    except (KeyError, ValueError):
        return None
    for j in meta.get("jars", []):
        try:
            inner = zipfile.ZipFile(io.BytesIO(z.read(j["path"])))
        except (KeyError, zipfile.BadZipFile):
            continue
        found = read_neoforge(inner, j.get("version", {}).get("artifactVersion"))
        if found:
            return found
    return None


def scan_jar(path):
    info = {"file": path.name, "size_mb": round(path.stat().st_size / 1048576, 2)}
    try:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            found = read_neoforge(z, file_name_version(path.name))
            if not found and "fabric.mod.json" not in names:
                found = read_nested(z)
            if found:
                info.update(found)
                return info
            if "fabric.mod.json" in names:
                d = json.loads(z.read("fabric.mod.json").decode("utf-8", "replace"), strict=False)
                info["loader"] = "fabric (via Connector)"
                info["modid"] = d.get("id", "?")
                info["name"] = d.get("name", info["modid"])
                info["version"] = d.get("version", "?")
                info["environment"] = d.get("environment", "*")
                return info
    except Exception as e:
        info["error"] = str(e)
    info.setdefault("loader", "?")
    info.setdefault("modid", "?")
    info.setdefault("name", path.stem)
    info.setdefault("version", "?")
    return info


def main():
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from build_packs import is_personal
    jars = sorted((p for p in MODS_DIR.glob("*.jar") if not is_personal(p.name)), key=lambda p: p.name.lower())
    inv = [scan_jar(p) for p in jars]
    OUT.write_text(json.dumps(inv, indent=1, ensure_ascii=False), encoding="utf-8")
    print(f"{len(inv)} jars -> {OUT}")
    bad = [i["file"] for i in inv if i.get("modid") == "?"]
    if bad:
        print("no metadata:", *bad, sep="\n  ")


if __name__ == "__main__":
    main()
