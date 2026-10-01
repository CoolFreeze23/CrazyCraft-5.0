"""Compare every file in pack/manifest.json with the newest version its source offers for Minecraft 1.21.1.

Usage:
  python scripts/check_updates.py [--out build/updates.md]

Modrinth entries are checked against the project's newest version for the same loader and 1.21.1; CurseForge
entries through the same proxy gen_manifest.py uses (cached ids in pack/cf_cache.json); our own GitHub mods against
their latest release; NeoForge against its Maven metadata. Nothing is downloaded or changed: the result is a report.
"""
import argparse
import json
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "pack"
UA = "CoolFreeze23/CrazyCraft-5.0 (github.com/CoolFreeze23/CrazyCraft-5.0)"
MODRINTH = "https://api.modrinth.com/v2"
CF_API = "https://api.curse.tools/v1"
NEOFORGE_LOADER_ID = 6  # CurseForge modLoaderType for NeoForge
MC = "1.21.1"


def http(url, data=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    if data is not None:
        req.data = json.dumps(data).encode()
        req.add_header("Content-Type", "application/json")
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < 3:
                time.sleep(10)
                continue
            if e.code == 404:
                return None
            raise
    return None


def modrinth_batch(kind, ids):
    out = {}
    for i in range(0, len(ids), 60):
        q = urllib.parse.quote(json.dumps(ids[i:i + 60]))
        for x in http(f"{MODRINTH}/{kind}?ids={q}") or []:
            out[x["id"]] = x
    return out


def newest(versions):
    versions = sorted(versions, key=lambda v: v["date_published"], reverse=True)
    rel = next((v for v in versions if v.get("version_type") == "release"), None)
    return (versions[0] if versions else None), rel


def day(iso):
    return iso[:10]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(ROOT / "build" / "updates.md"))
    args = ap.parse_args()
    manifest = json.loads((PACK / "manifest.json").read_text(encoding="utf-8"))
    cf_cache = json.loads((PACK / "cf_cache.json").read_text(encoding="utf-8")) if (PACK / "cf_cache.json").exists() else {}
    sources = json.loads((PACK / "sources.json").read_text(encoding="utf-8"))
    patched = set(sources.get("patched", {}))
    pins = json.loads((PACK / "pins.json").read_text(encoding="utf-8")) if (PACK / "pins.json").exists() else {}
    rows = []  # dicts: file, name, kind, current, latest, latest_release, date, status, notes

    # Modrinth
    mr = [m for m in manifest["mods"] if m["source"]["type"] == "modrinth"]
    ids = {}
    for m in mr:
        pid, vid = re.search(r"/data/([A-Za-z0-9]+)/versions/([A-Za-z0-9]+)/", m["source"]["url"]).groups()
        ids[m["file"]] = (pid, vid)
    cur = modrinth_batch("versions", sorted({v for _, v in ids.values()}))
    projects = modrinth_batch("projects", sorted({p for p, _ in ids.values()}))
    pack_project_ids = set(projects)
    for n, m in enumerate(mr, 1):
        pid, vid = ids[m["file"]]
        c = cur.get(vid)
        proj = projects.get(pid, {})
        loaders = c.get("loaders", []) if c else []
        is_pack = m.get("dir") == "resourcepacks" or proj.get("project_type") == "resourcepack"
        if is_pack:
            lst = http(f"{MODRINTH}/project/{pid}/version") or []
            lst = [v for v in lst if any(g in (MC, "1.21") for g in v.get("game_versions", []))]
        else:
            q = urllib.parse.quote(json.dumps(loaders)) if loaders else None
            url = f"{MODRINTH}/project/{pid}/version?game_versions={urllib.parse.quote(json.dumps([MC]))}"
            if q:
                url += f"&loaders={q}"
            lst = http(url) or []
        time.sleep(0.22)
        top, rel = newest(lst)
        row = {"file": m["file"], "name": m.get("name") or proj.get("title", ""), "kind": "resource pack" if is_pack else ("fabric (Connector)" if "fabric" in loaders and "neoforge" not in loaders else "neoforge"),
               "current": c["version_number"] if c else m.get("version", ""), "current_date": day(c["date_published"]) if c else "",
               "page": m["source"].get("page", ""), "notes": []}
        if m["file"] in patched:
            row["notes"].append("patched on install; edit list must be re-derived")
        if proj.get("slug") in pins:
            row["notes"].append("held back: " + pins[proj["slug"]])
        if not top or top["id"] == vid or (c and top["date_published"] <= c["date_published"]):
            row.update(status="current", latest=row["current"], latest_date=row["current_date"])
        else:
            row.update(status="update", latest=top["version_number"], latest_date=day(top["date_published"]), latest_type=top.get("version_type"),
                       latest_file=(top["files"][0]["filename"] if top.get("files") else ""))
            if rel and rel["id"] != top["id"] and (not c or rel["date_published"] > c["date_published"]):
                row["notes"].append(f"newest release {rel['version_number']} ({day(rel['date_published'])}); newest overall is {top.get('version_type')}")
            elif top.get("version_type") != "release":
                row["notes"].append(f"newest is a {top.get('version_type')}")
            new_deps = [d for d in top.get("dependencies", []) if d.get("dependency_type") == "required" and d.get("project_id") and d["project_id"] not in pack_project_ids]
            if new_deps:
                names = modrinth_batch("projects", [d["project_id"] for d in new_deps])
                row["notes"].append("new required dependency: " + ", ".join(names[d["project_id"]]["title"] if d["project_id"] in names else d["project_id"] for d in new_deps))
            if top.get("game_versions") and MC not in top["game_versions"]:
                row["notes"].append("lists 1.21, not 1.21.1")
        rows.append(row)
        print(f"\r{n}/{len(mr)} modrinth", end="", file=sys.stderr)
    print(file=sys.stderr)

    # CurseForge (mods and the two resource packs) and the manual MCHeli entry
    for m in manifest["mods"]:
        t = m["source"]["type"]
        if t not in ("curseforge", "manual"):
            continue
        c = cf_cache.get(m["sha512"])
        row = {"file": m["file"], "name": m.get("name", ""), "kind": "curseforge" if t == "curseforge" else "curseforge (manual)",
               "current": m.get("version", ""), "current_date": "", "page": m["source"].get("page", ""), "notes": []}
        if m["file"] in patched:
            row["notes"].append("patched on install; edit list must be re-derived")
        if not c:
            row.update(status="unknown", latest="", latest_date="")
            row["notes"].append("not in cf_cache.json")
            rows.append(row)
            continue
        is_pack = m.get("dir") == "resourcepacks"
        url = f"{CF_API}/mods/{c['project']}/files?gameVersion={MC}&pageSize=50" + ("" if is_pack else f"&modLoaderType={NEOFORGE_LOADER_ID}")
        files = (http(url) or {}).get("data", [])
        files.sort(key=lambda f: f["fileDate"], reverse=True)
        curf = next((f for f in files if f["id"] == c["file"]), None)
        row["current_date"] = day(curf["fileDate"]) if curf else ""
        if not files or files[0]["id"] == c["file"] or (curf and files[0]["fileDate"] <= curf["fileDate"]):
            row.update(status="current", latest=row["current"], latest_date=row["current_date"])
        else:
            top = files[0]
            rtype = {1: "release", 2: "beta", 3: "alpha"}.get(top.get("releaseType"), "?")
            row.update(status="update", latest=top["displayName"], latest_date=day(top["fileDate"]), latest_type=rtype, latest_file=top["fileName"])
            if rtype != "release":
                row["notes"].append(f"newest is a {rtype}")
            if t == "manual":
                row["notes"].append("author allows CurseForge downloads only; fetched by hand")
        rows.append(row)

    # Our own mods on GitHub
    for m in manifest["mods"]:
        if m["source"]["type"] != "github":
            continue
        repo = re.search(r"github\.com/([^/]+/[^/]+)", m["source"]["page"]).group(1)
        try:
            rel = json.loads(subprocess.run(["gh", "api", f"repos/{repo}/releases/latest"], capture_output=True, text=True, check=True).stdout)
            tag, date = rel["tag_name"], day(rel["published_at"])
        except Exception:
            tag, date = "?", ""
        inpack = m.get("version", "")
        same = tag.lstrip("v") in inpack or inpack in tag.lstrip("v")
        rows.append({"file": m["file"], "name": m.get("name", ""), "kind": "own mod (GitHub)", "current": inpack, "current_date": "",
                     "latest": tag, "latest_date": date, "status": "current" if same else "update", "page": m["source"]["page"],
                     "notes": [] if same else ["latest release on GitHub differs from the jar in the pack"]})

    # NeoForge
    meta = urllib.request.urlopen(urllib.request.Request("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml", headers={"User-Agent": UA}), timeout=60).read()
    versions = [v.text for v in ET.fromstring(meta).iter("version") if v.text.startswith("21.1.")]
    versions.sort(key=lambda s: [int(x) for x in s.split(".")[:3]] if s.replace(".", "").isdigit() else [0])
    nf_cur, nf_new = manifest["neoforge"]["version"], versions[-1]
    rows.append({"file": "NeoForge", "name": "NeoForge", "kind": "loader", "current": nf_cur, "current_date": "", "latest": nf_new, "latest_date": "",
                 "status": "current" if nf_cur == nf_new else "update", "page": "https://neoforged.net", "notes": []})

    # Report
    upd = [r for r in rows if r["status"] == "update"]
    cur_n = sum(1 for r in rows if r["status"] == "current")
    unk = [r for r in rows if r["status"] == "unknown"]
    out = [f"# Update check, {datetime.now(timezone.utc).strftime('%Y-%m-%d')}", "",
           f"{len(rows)} files checked: {len(upd)} with a newer version, {cur_n} current, {len(unk)} unknown.", "",
           "## Newer versions available", "", "| Mod | In pack | Newest | Type | Notes |", "|---|---|---|---|---|"]
    for r in sorted(upd, key=lambda r: (r["kind"], r["name"].lower())):
        out.append(f"| [{r['name']}]({r['page']}) ({r['kind']}) | {r['current']} ({r['current_date']}) | {r['latest']} ({r['latest_date']}) | {r.get('latest_type', '')} | {'; '.join(r['notes'])} |")
    if unk:
        out += ["", "## Could not check", ""] + [f"- {r['file']}: {'; '.join(r['notes'])}" for r in unk]
    out += ["", "## Current", "", ", ".join(sorted(r["name"] for r in rows if r["status"] == "current"))]
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text("\n".join(out) + "\n", encoding="utf-8")
    (Path(args.out).with_suffix(".json")).write_text(json.dumps(rows, indent=1), encoding="utf-8")
    print("\n".join(out[:4]))
    print(f"report: {args.out}")


if __name__ == "__main__":
    main()
