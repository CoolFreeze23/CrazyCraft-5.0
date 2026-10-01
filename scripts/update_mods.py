"""Move every mod and resource pack in the instance to the newest build its source offers for Minecraft 1.21.1,
and pull in any dependency the new builds need.

Usage:
  python scripts/update_mods.py plan            # resolve targets and missing dependencies, write build/update-plan.json
  python scripts/update_mods.py apply           # download and install what the plan says (old jars go to disabled-mods/)

Rules: a newer release always wins; a beta or alpha is taken only when the mod ships nothing else (its current build is
already a pre-release, or it is one of the projects that only publish pre-releases). Patched mods (pack/sources.json)
are downloaded untouched to build/update-originals/ and left for the patch step. Our own mods on GitHub are skipped, and so are the projects held back in pack/pins.json (with the reason why).
"""
import hashlib
import json
import re
import shutil
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_packs import MC  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "pack"
BUILD = ROOT / "build"
UA = "CoolFreeze23/CrazyCraft-5.0 (github.com/CoolFreeze23/CrazyCraft-5.0)"
MODRINTH = "https://api.modrinth.com/v2"
CF_API = "https://api.curse.tools/v1"
MCV = "1.21.1"
NEOFORGE_CF = 6
PRERELEASE_OK = {"fresh-animations", "serene-seasons", "accessories", "connector", "owo-lib"}
# a dependency on the Fabric API is met by the Forgified Fabric API that Connector brings
EQUIVALENT = {"P7dR8mSH": "Aqlf1Shp"}
SKIP_DEPS = {"Pb3OXVqC"}  # Sodium: pinned by Iris, handled by hand


def http(url, data=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    if data is not None:
        req.data = json.dumps(data).encode()
        req.add_header("Content-Type", "application/json")
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=90) as r:
                return json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(15)
                continue
            if e.code == 404:
                return None
            raise
    raise SystemExit(f"gave up on {url}")


def batch(kind, ids):
    out = {}
    ids = sorted(set(ids))
    for i in range(0, len(ids), 60):
        q = urllib.parse.quote(json.dumps(ids[i:i + 60]))
        for x in http(f"{MODRINTH}/{kind}?ids={q}") or []:
            out[x["id"]] = x
    return out


def pick(versions, current, slug):
    """Newest release newer than the current build, else the newest pre-release when that is all the project has."""
    newer = [v for v in versions if not current or v["date_published"] > current["date_published"]]
    newer.sort(key=lambda v: v["date_published"], reverse=True)
    rel = next((v for v in newer if v["version_type"] == "release"), None)
    if rel:
        return rel
    if newer and ((current and current["version_type"] != "release") or slug in PRERELEASE_OK):
        return newer[0]
    return None


def primary(v):
    return next((f for f in v["files"] if f.get("primary")), v["files"][0])


def digest(path, algo):
    h = hashlib.new(algo)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def download(url, dest, sha512=None, sha1=None):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists() and (not sha512 or digest(dest, "sha512") == sha512):
        return dest
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=300) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)
    if sha512 and digest(dest, "sha512") != sha512:
        raise SystemExit(f"bad sha512 for {dest.name}")
    if sha1 and digest(dest, "sha1") != sha1:
        raise SystemExit(f"bad sha1 for {dest.name}")
    return dest


def plan():
    manifest = json.loads((PACK / "manifest.json").read_text(encoding="utf-8"))
    sources = json.loads((PACK / "sources.json").read_text(encoding="utf-8"))
    cf_cache = json.loads((PACK / "cf_cache.json").read_text(encoding="utf-8"))
    patched = set(sources.get("patched", {}))
    pins = json.loads((PACK / "pins.json").read_text(encoding="utf-8")) if (PACK / "pins.json").exists() else {}
    entries = manifest["mods"]
    items = []  # one per manifest entry with a Modrinth source
    for e in entries:
        if e["source"]["type"] == "modrinth":
            pid, vid = re.search(r"/data/([A-Za-z0-9]+)/versions/([A-Za-z0-9]+)/", e["source"]["url"]).groups()
            items.append({"entry": e, "pid": pid, "vid": vid})
    cur = batch("versions", [i["vid"] for i in items])
    projects = batch("projects", [i["pid"] for i in items])
    present = {i["pid"] for i in items}  # project ids in the pack (before the update)
    final_versions = {}  # pid -> version object the pack will end on
    updates = []
    for n, i in enumerate(items, 1):
        e, pid, vid = i["entry"], i["pid"], i["vid"]
        c = cur.get(vid)
        proj = projects[pid]
        is_pack = e.get("dir") == "resourcepacks"
        if is_pack:
            vs = [v for v in (http(f"{MODRINTH}/project/{pid}/version") or []) if any(g in (MCV, "1.21") for g in v["game_versions"])]
        else:
            loaders = [l for l in (c["loaders"] if c else ["neoforge"]) if l in ("neoforge", "fabric")]
            if "neoforge" in loaders:
                loaders = ["neoforge"]
            q = urllib.parse.quote(json.dumps(loaders))
            vs = http(f"{MODRINTH}/project/{pid}/version?loaders={q}&game_versions={urllib.parse.quote(json.dumps([MCV]))}") or []
        time.sleep(0.2)
        t = None if proj["slug"] in pins else pick(vs, c, proj["slug"])
        final_versions[pid] = t or c
        if t and t["id"] != vid:
            f = primary(t)
            updates.append({"file": e["file"], "name": e["name"], "dir": e.get("dir", "mods"), "project": pid, "slug": proj["slug"],
                            "from": c["version_number"] if c else e.get("version"), "to": t["version_number"], "type": t["version_type"],
                            "date": t["date_published"][:10], "version_id": t["id"], "url": f["url"], "new_file": f["filename"],
                            "sha512": f["hashes"]["sha512"], "patched": e["file"] in patched, "source": "modrinth"})
        print(f"\r{n}/{len(items)} resolved", end="", file=sys.stderr)
    print(file=sys.stderr)

    # CurseForge entries
    for e in entries:
        if e["source"]["type"] not in ("curseforge", "manual"):
            continue
        c = cf_cache.get(e["sha512"])
        if not c:
            continue
        is_pack = e.get("dir") == "resourcepacks"
        url = f"{CF_API}/mods/{c['project']}/files?gameVersion={MCV}&pageSize=50" + ("" if is_pack else f"&modLoaderType={NEOFORGE_CF}")
        files = sorted((http(url) or {}).get("data", []), key=lambda f: f["fileDate"], reverse=True)
        curf = next((f for f in files if f["id"] == c["file"]), None)
        newer = [f for f in files if not curf or f["fileDate"] > curf["fileDate"]]
        rel = next((f for f in newer if f.get("releaseType") == 1), None) or (newer[0] if newer and curf and curf.get("releaseType") != 1 else None)
        if not rel:
            continue
        sha1 = next((h["value"] for h in rel.get("hashes", []) if h.get("algo") == 1), None)
        dl = rel.get("downloadUrl") or f"https://www.curseforge.com/api/v1/mods/{c['project']}/files/{rel['id']}/download"
        updates.append({"file": e["file"], "name": e["name"], "dir": e.get("dir", "mods"), "project": str(c["project"]), "slug": c["slug"],
                        "from": e.get("version"), "to": rel["displayName"], "type": {1: "release", 2: "beta", 3: "alpha"}.get(rel.get("releaseType"), "?"),
                        "date": rel["fileDate"][:10], "file_id": rel["id"], "url": dl, "new_file": rel["fileName"], "sha1": sha1,
                        "patched": e["file"] in patched, "source": "curseforge" if rel.get("downloadUrl") else "manual"})

    # dependencies the final set needs and the pack does not have
    present_after = set(present)
    need = {}  # pid -> (needed by, loader)
    for pid, v in final_versions.items():
        if not v:
            continue
        for d in v.get("dependencies", []):
            dp = d.get("project_id")
            if not dp or d.get("dependency_type") != "required":
                continue
            dp = EQUIVALENT.get(dp, dp)
            if dp in present_after or dp in SKIP_DEPS or dp == pid:
                continue
            need.setdefault(dp, []).append((projects[pid]["title"], v["loaders"]))
    added = []
    unresolved = []
    for dp, by in need.items():
        proj = http(f"{MODRINTH}/project/{dp}")
        if not proj:
            unresolved.append((dp, by))
            continue
        loaders = ["neoforge"] if "neoforge" in proj["loaders"] else (["fabric"] if "fabric" in proj["loaders"] else [])
        vs = []
        if loaders:
            vs = http(f"{MODRINTH}/project/{dp}/version?loaders={urllib.parse.quote(json.dumps(loaders))}&game_versions={urllib.parse.quote(json.dumps([MCV]))}") or []
        t = pick(vs, None, proj["slug"])
        if not t:
            unresolved.append((proj["title"], by))
            continue
        f = primary(t)
        side = "client" if proj.get("server_side") == "unsupported" else "both"
        added.append({"name": proj["title"], "slug": proj["slug"], "project": dp, "to": t["version_number"], "type": t["version_type"], "date": t["date_published"][:10],
                      "loader": loaders[0], "side": side, "needed_by": sorted({b for b, _ in by}), "url": f["url"], "new_file": f["filename"],
                      "sha512": f["hashes"]["sha512"], "dir": "mods", "source": "modrinth"})
        present_after.add(dp)
        time.sleep(0.2)
    out = {"updates": updates, "added": added, "unresolved": unresolved}
    BUILD.mkdir(exist_ok=True)
    (BUILD / "update-plan.json").write_text(json.dumps(out, indent=1), encoding="utf-8")
    show()


def show():
    p = json.loads((BUILD / "update-plan.json").read_text(encoding="utf-8"))
    updates, added, unresolved = p["updates"], p["added"], p["unresolved"]
    print(f"{len(updates)} updates, {len(added)} dependencies to add, {len(unresolved)} unresolved")
    for u in sorted(updates, key=lambda u: (u["dir"], u["name"].lower())):
        flags = " [patched]" if u["patched"] else ""
        flags += " [manual]" if u["source"] == "manual" else ""
        print(f"  {u['name']}: {u['from']} -> {u['to']} ({u['type']}, {u['date']}) {u['new_file']}{flags}")
    for a in added:
        print(f"  + {a['name']} {a['to']} ({a['loader']}, {a['side']}) needed by {', '.join(a['needed_by'])}")
    for u in unresolved:
        print(f"  ! unresolved dependency {u[0]} needed by {[b for b, _ in u[1]]}")


def apply():
    p = json.loads((BUILD / "update-plan.json").read_text(encoding="utf-8"))
    staging = BUILD / "update-downloads"
    originals = BUILD / "update-originals"
    old_packs = BUILD / "update-replaced-resourcepacks"
    log = {"replaced": [], "added": [], "skipped": []}
    for u in p["updates"] + p["added"]:
        dest = staging / u["new_file"]
        try:
            download(u["url"], dest, sha512=u.get("sha512"), sha1=u.get("sha1"))
        except urllib.error.HTTPError as e:
            log["skipped"].append({"file": u.get("file"), "new_file": u["new_file"], "why": f"HTTP {e.code} from {u['url']}"})
            print(f"  skipped {u['new_file']}: HTTP {e.code}")
            continue
        if u.get("patched"):
            shutil.copy2(dest, originals / u["new_file"]) if (originals.mkdir(parents=True, exist_ok=True) or True) else None
            log["skipped"].append({"file": u["file"], "new_file": u["new_file"], "why": "patched mod, original staged for the patch step"})
            print(f"  staged original for patching: {u['new_file']}")
            continue
        target_dir = MC / u["dir"]
        new = target_dir / u["new_file"]
        old = target_dir / u["file"] if u.get("file") else None
        if new.exists() and digest(new, "sha512") == digest(dest, "sha512") and (old is None or not old.exists() or old == new):
            print(f"  already installed {u['new_file']}")
            (log["replaced"] if old else log["added"]).append({"old": u.get("file"), "new": u["new_file"], "dir": u["dir"], "side": u.get("side", "both"), "name": u["name"]})
            continue
        try:
            if old and old.exists() and old != new:
                if u["dir"] == "mods":
                    (MC / "disabled-mods").mkdir(exist_ok=True)
                    shutil.move(str(old), str(MC / "disabled-mods" / u["file"]))
                else:
                    old_packs.mkdir(parents=True, exist_ok=True)
                    shutil.move(str(old), str(old_packs / u["file"]))
            shutil.copy2(dest, new)
        except PermissionError as e:
            log["skipped"].append({"file": u.get("file"), "new_file": u["new_file"], "why": f"file in use: {e.filename}"})
            print(f"  LOCKED, left for later: {u['new_file']}")
            continue
        if old:
            log["replaced"].append({"old": u["file"], "new": u["new_file"], "dir": u["dir"]})
        else:
            log["added"].append({"new": u["new_file"], "dir": u["dir"], "side": u.get("side", "both"), "name": u["name"]})
        print(f"  installed {u['new_file']}" + (f" (replaces {u['file']})" if u.get("file") else " (new)"))
        (BUILD / "update-applied.json").write_text(json.dumps(log, indent=1), encoding="utf-8")
    (BUILD / "update-applied.json").write_text(json.dumps(log, indent=1), encoding="utf-8")
    print(f"replaced {len(log['replaced'])}, added {len(log['added'])}, skipped {len(log['skipped'])}; details in build/update-applied.json")


if __name__ == "__main__":
    {"plan": plan, "apply": apply, "show": show}[sys.argv[1] if len(sys.argv) > 1 else "plan"]()
