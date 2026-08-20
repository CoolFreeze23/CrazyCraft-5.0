"""Scan the instance mods folder and emit a JSON inventory of every jar.

Reads neoforge.mods.toml / fabric.mod.json out of each jar to get mod id,
display name, version, loader, and (for fabric) declared environment.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

MODS_DIR = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\instances\CrazyCraft 5.0\minecraft\mods")
OUT = Path(__file__).resolve().parent.parent / "docs" / "mod_inventory.json"


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


def scan_jar(path):
    info = {"file": path.name, "size_mb": round(path.stat().st_size / 1048576, 2)}
    try:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            toml_name = None
            for cand in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if cand in names:
                    toml_name = cand
                    break
            if toml_name:
                text = z.read(toml_name).decode("utf-8", "replace")
                mods = parse_toml_mods(text)
                if mods:
                    m = mods[0]
                    info["loader"] = "neoforge"
                    info["modid"] = m.get("modId", "?")
                    info["name"] = m.get("displayName", info["modid"])
                    v = m.get("version", "?")
                    if "${" in v:  # version substituted from manifest
                        try:
                            mf = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
                            mv = re.search(r"Implementation-Version:\s*(\S+)", mf)
                            v = mv.group(1) if mv else "?"
                        except KeyError:
                            v = "?"
                    info["version"] = v
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
    jars = sorted(MODS_DIR.glob("*.jar"), key=lambda p: p.name.lower())
    inv = [scan_jar(p) for p in jars]
    OUT.write_text(json.dumps(inv, indent=1, ensure_ascii=False), encoding="utf-8")
    print(f"{len(inv)} jars -> {OUT}")
    bad = [i["file"] for i in inv if i.get("modid") == "?"]
    if bad:
        print("no metadata:", *bad, sep="\n  ")


if __name__ == "__main__":
    main()
