"""Build pack/manifest.json: where every mod in the pack is downloaded from, and how to check it.

The pack does not redistribute other people's mods. The server loader (loader/) reads this manifest and downloads
each jar from its official source: Modrinth, CurseForge (only where the author allows third-party downloads) or the
GitHub releases of our own mods. Mods whose author only allows downloads from CurseForge itself are marked "manual".
A few published mods need a small fix to work in the pack; for those the manifest points at the untouched original
and lists the edits, which the loader applies after the download (see "patched" in pack/sources.json).

Usage:
  python scripts/gen_manifest.py --version 1.2.9 [--check-urls]

Sources, in order: pack/sources.json (our own mods, the patched mods, extra files), an exact-file lookup on Modrinth
(by SHA-512), then CurseForge (by fingerprint, cached in pack/cf_cache.json).
"""
import argparse
import hashlib
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_packs import MC, NEOFORGE_VERSION, is_excluded  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "pack"
CACHE = ROOT / "build" / "cache"
UA = "CoolFreeze23/CrazyCraft-5.0 (github.com/CoolFreeze23/CrazyCraft-5.0)"
CF_API = "https://api.curse.tools/v1"
MODRINTH = "https://api.modrinth.com/v2"


def http(url, data=None, raw=False, method=None, headers=None):
    h = {"User-Agent": UA, "Accept": "application/json"}
    if data is not None:
        h["Content-Type"] = "application/json"
        data = json.dumps(data).encode()
    h.update(headers or {})
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    with urllib.request.urlopen(req, timeout=120) as r:
        body = r.read()
    return body if raw else json.loads(body)


def sha(data, algo):
    return hashlib.new(algo, data).hexdigest()


def cf_fingerprint(data):
    """CurseForge's file fingerprint: MurmurHash2 (seed 1) over the bytes without whitespace."""
    b = data.translate(None, b"\t\n\r ")
    m = 0x5BD1E995
    n = len(b)
    h = (1 ^ n) & 0xFFFFFFFF
    i = 0
    while n - i >= 4:
        k = int.from_bytes(b[i:i + 4], "little")
        k = (k * m) & 0xFFFFFFFF
        k ^= k >> 24
        k = (k * m) & 0xFFFFFFFF
        h = ((h * m) & 0xFFFFFFFF) ^ k
        i += 4
    rest = n - i
    if rest == 3:
        h ^= b[i + 2] << 16
    if rest >= 2:
        h ^= b[i + 1] << 8
    if rest >= 1:
        h ^= b[i]
        h = (h * m) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * m) & 0xFFFFFFFF
    h ^= h >> 15
    return h


def zip_files(zf):
    """Name -> bytes for every file entry (directories left out), in the zip's own order."""
    return {i.filename: zf.read(i) for i in zf.infolist() if not i.filename.endswith("/")}


def content_hash(files):
    """SHA-256 over the sorted "name<TAB>sha256(content)" lines of a jar's files; the loader computes the same."""
    lines = sorted(f"{name}\t{sha(data, 'sha256')}\n" for name, data in files.items())
    return sha("".join(lines).encode("utf-8"), "sha256")


def is_text(name, a, b):
    if name.endswith(".class"):
        return False
    try:
        a.decode("utf-8")
        b.decode("utf-8")
        return True
    except UnicodeDecodeError:
        return False


def derive_ops(orig, ours):
    """The smallest list of edits that turns the original jar's files into ours."""
    ops = []
    for name in orig:
        if name not in ours:
            ops.append({"op": "remove", "entry": name})
    for name in ours:
        if name not in orig:
            data = ours[name]
            ops.append({"op": "add", "entry": name, "text": data.decode("utf-8")})
    for name in orig:
        if name not in ours or orig[name] == ours[name]:
            continue
        a, b = orig[name], ours[name]
        if len(a) == len(b) and not is_text(name, a, b):
            runs, i = [], 0
            while i < len(a):
                if a[i] == b[i]:
                    i += 1
                    continue
                j = i
                while j < len(a) and (a[j] != b[j] or any(a[k] != b[k] for k in range(j, min(j + 8, len(a))))):
                    j += 1
                runs.append((i, j))
                i = j
            for lo, hi in runs:
                ops.append({"op": "bytes", "entry": name, "offset": lo, "expect": a[lo:hi].hex(), "replace": b[lo:hi].hex()})
            continue
        if not is_text(name, a, b):
            raise SystemExit(f"{name}: binary entry changes size; a patch can't describe it")
        p = 0
        while p < min(len(a), len(b)) and a[p] == b[p]:
            p += 1
        s = 0
        while s < min(len(a), len(b)) - p and a[len(a) - 1 - s] == b[len(b) - 1 - s]:
            s += 1
        lo, hi = p, len(a) - s
        while True:
            find = a[lo:hi]
            if find and a.count(find) == 1:
                break
            if lo == 0 and hi == len(a):
                break
            lo, hi = max(0, lo - 8), min(len(a), hi + 8)
        ops.append({"op": "replace", "entry": name, "find": a[lo:hi].decode("utf-8"),
                    "with": b[lo:hi + len(b) - len(a)].decode("utf-8")})
    return ops


def apply_ops(files, ops):
    files = dict(files)
    for op in ops:
        e = op["entry"]
        if op["op"] == "remove":
            del files[e]
        elif op["op"] == "add":
            files[e] = op["text"].encode("utf-8")
        elif op["op"] == "bytes":
            data = bytearray(files[e])
            off, expect, repl = op["offset"], bytes.fromhex(op["expect"]), bytes.fromhex(op["replace"])
            if data[off:off + len(expect)] != expect:
                raise ValueError(f"{e}: unexpected bytes at {off}")
            data[off:off + len(repl)] = repl
            files[e] = bytes(data)
        elif op["op"] == "replace":
            data, find = files[e], op["find"].encode("utf-8")
            if data.count(find) != 1:
                raise ValueError(f"{e}: text to replace found {data.count(find)} times")
            files[e] = data.replace(find, op["with"].encode("utf-8"), 1)
    return files


def cached_download(url, sha512, name):
    CACHE.mkdir(parents=True, exist_ok=True)
    dest = CACHE / f"{sha512[:16]}-{name}"
    if not dest.exists():
        dest.write_bytes(http(url, raw=True))
    data = dest.read_bytes()
    if sha(data, "sha512") != sha512:
        dest.unlink()
        raise SystemExit(f"{name}: download does not match its published SHA-512")
    return data


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True, help="pack version this manifest belongs to, like 1.2.9")
    ap.add_argument("--check-urls", action="store_true", help="also check that every download link answers")
    args = ap.parse_args()

    sources = json.loads((PACK / "sources.json").read_text(encoding="utf-8"))
    cf_cache_path = PACK / "cf_cache.json"
    cf_cache = json.loads(cf_cache_path.read_text(encoding="utf-8")) if cf_cache_path.exists() else {}
    inventory = {r["file"]: r for r in json.loads((ROOT / "docs" / "mod_inventory.json").read_text(encoding="utf-8"))}

    jars = sorted(p for p in (MC / "mods").glob("*.jar"))
    mods = []
    for p in jars:
        data = p.read_bytes()
        inv = inventory.get(p.name, {})
        mods.append({"file": p.name, "name": inv.get("name") or p.stem, "version": inv.get("version") or "",
                     "side": "client" if is_excluded(p.name) else "both",
                     "size": len(data), "sha512": sha(data, "sha512"), "_path": p})
    print(f"{len(mods)} jars in the instance, {sum(m['side'] == 'both' for m in mods)} on the server")

    # 1. our own mods on GitHub
    for m in mods:
        for g in sources["github"]:
            hit = re.match(g["pattern"], m["file"])
            if hit:
                url = g["url"].replace("{file}", urllib.parse.quote(m["file"]))
                for i, grp in enumerate(hit.groups(), 1):
                    url = url.replace("{%d}" % i, grp)
                m["source"] = {"type": "github", "url": url, "page": g["page"]}
                m["license"] = g.get("license", "")
                break

    # 2. patched mods: the untouched original from Modrinth, plus the edits
    for m in mods:
        spec = sources["patched"].get(m["file"])
        if not spec:
            continue
        v = http(f"{MODRINTH}/version/{spec['modrinthVersion']}")
        f = next(f for f in v["files"] if f["filename"] == spec["originalFile"])
        orig = cached_download(f["url"], f["hashes"]["sha512"], f["filename"])
        with zipfile.ZipFile(m["_path"]) as zo, zipfile.ZipFile(CACHE / f"{f['hashes']['sha512'][:16]}-{f['filename']}") as za:
            ours_files, orig_files = zip_files(zo), zip_files(za)
        ops = derive_ops(orig_files, ours_files)
        result = content_hash(apply_ops(orig_files, ops))
        if result != content_hash(ours_files):
            raise SystemExit(f"{m['file']}: the derived patch does not rebuild the pack's jar")
        proj = http(f"{MODRINTH}/project/{v['project_id']}")
        m.update({"size": f["size"], "sha512": f["hashes"]["sha512"]})
        m["source"] = {"type": "modrinth", "url": f["url"], "page": f"https://modrinth.com/{proj['project_type']}/{proj['slug']}"}
        m["license"] = (proj.get("license") or {}).get("id", "")
        m["patch"] = {"originalFile": f["filename"], "ops": ops, "result": result, "why": spec["why"]}
        print(f"  patched {m['file']}: {len(ops)} edit(s) on {f['filename']}, rebuild verified")

    # 3. exact files on Modrinth
    todo = [m for m in mods if "source" not in m]
    found = http(f"{MODRINTH}/version_files", data={"hashes": [m["sha512"] for m in todo], "algorithm": "sha512"})
    projects = {}
    ids = sorted({v["project_id"] for v in found.values()})
    for i in range(0, len(ids), 80):
        q = urllib.parse.quote(json.dumps(ids[i:i + 80]))
        for proj in http(f"{MODRINTH}/projects?ids={q}"):
            projects[proj["id"]] = proj
    for m in todo:
        v = found.get(m["sha512"])
        if not v:
            continue
        f = next(f for f in v["files"] if f["hashes"]["sha512"] == m["sha512"])
        proj = projects[v["project_id"]]
        m["source"] = {"type": "modrinth", "url": f["url"], "page": f"https://modrinth.com/{proj['project_type']}/{proj['slug']}"}
        m["license"] = (proj.get("license") or {}).get("id", "")
        if not inventory.get(m["file"], {}).get("name"):
            m["name"] = proj["title"]

    # 4. CurseForge, by fingerprint (cached, keyed by SHA-512)
    todo = [m for m in mods if "source" not in m]
    missing = [m for m in todo if m["sha512"] not in cf_cache]
    if missing:
        fps = {cf_fingerprint(m["_path"].read_bytes()): m for m in missing}
        res = http(f"{CF_API}/fingerprints/432", data={"fingerprints": list(fps)})["data"]
        hits = {x["file"]["fileFingerprint"]: x for x in res.get("exactMatches", [])}
        pids = sorted({x["id"] for x in hits.values()})
        cf_mods = {p["id"]: p for p in http(f"{CF_API}/mods", data={"modIds": pids})["data"]} if pids else {}
        for fp, m in fps.items():
            x = hits.get(fp)
            if not x:
                continue
            p, f = cf_mods[x["id"]], x["file"]
            cf_cache[m["sha512"]] = {"project": x["id"], "file": f["id"], "fileName": f["fileName"], "slug": p["slug"],
                                     "name": p["name"], "allowModDistribution": p.get("allowModDistribution"),
                                     "downloadUrl": f.get("downloadUrl")}
        cf_cache_path.write_text(json.dumps(cf_cache, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    for m in todo:
        c = cf_cache.get(m["sha512"])
        if not c:
            continue
        page = f"https://www.curseforge.com/minecraft/mc-mods/{c['slug']}"
        if c["allowModDistribution"] and c["downloadUrl"]:
            m["source"] = {"type": "curseforge", "url": c["downloadUrl"], "page": page}
        else:
            m["source"] = {"type": "manual", "url": None, "page": f"{page}/files/{c['file']}",
                           "download": f"{page}/download/{c['file']}",
                           "note": sources.get("manualNotes", {}).get(c["slug"],
                                                                     "Its author only allows downloads from CurseForge itself.")}
        m["license"] = ""

    unresolved = [m["file"] for m in mods if "source" not in m]
    if unresolved:
        raise SystemExit(f"No download source for: {unresolved}")

    # resource packs (client only): our own ship in the pack, the rest come from Modrinth or CurseForge
    rp_spec = sources.get("resourcepacks", {})
    packs = []
    for p in sorted((MC / "resourcepacks").glob("*.zip")):
        if p.name in rp_spec.get("bundled", []) or p.name in rp_spec.get("dropped", {}):
            continue
        data = p.read_bytes()
        packs.append({"file": p.name, "dir": "resourcepacks", "name": p.stem, "version": "", "side": "client",
                      "size": len(data), "sha512": sha(data, "sha512"), "_path": p})
    for pk in packs:
        sub = rp_spec.get("substitute", {}).get(pk["file"])
        if not sub:
            continue
        v = http(f"{MODRINTH}/version/{sub['modrinthVersion']}")
        f = next((f for f in v["files"] if f["filename"] == pk["file"]), v["files"][0])
        proj = http(f"{MODRINTH}/project/{v['project_id']}")
        pk.update({"size": f["size"], "sha512": f["hashes"]["sha512"], "name": proj["title"],
                   "version": v["version_number"], "license": (proj.get("license") or {}).get("id", "")})
        pk["source"] = {"type": "modrinth", "url": f["url"], "page": f"https://modrinth.com/{proj['project_type']}/{proj['slug']}"}
    todo = [pk for pk in packs if "source" not in pk]
    if todo:
        found = http(f"{MODRINTH}/version_files", data={"hashes": [pk["sha512"] for pk in todo], "algorithm": "sha512"})
        ids = sorted({v["project_id"] for v in found.values()})
        rp_projects = {}
        for i in range(0, len(ids), 80):
            q = urllib.parse.quote(json.dumps(ids[i:i + 80]))
            for proj in http(f"{MODRINTH}/projects?ids={q}"):
                rp_projects[proj["id"]] = proj
        for pk in todo:
            v = found.get(pk["sha512"])
            if not v:
                continue
            f = next(f for f in v["files"] if f["hashes"]["sha512"] == pk["sha512"])
            proj = rp_projects[v["project_id"]]
            pk.update({"name": proj["title"], "version": v["version_number"],
                       "license": (proj.get("license") or {}).get("id", "")})
            pk["source"] = {"type": "modrinth", "url": f["url"], "page": f"https://modrinth.com/{proj['project_type']}/{proj['slug']}"}
    todo = [pk for pk in packs if "source" not in pk]
    missing = [pk for pk in todo if pk["sha512"] not in cf_cache]
    if missing:
        fps = {cf_fingerprint(pk["_path"].read_bytes()): pk for pk in missing}
        res = http(f"{CF_API}/fingerprints/432", data={"fingerprints": list(fps)})["data"]
        hits = {x["file"]["fileFingerprint"]: x for x in res.get("exactMatches", [])}
        pids = sorted({x["id"] for x in hits.values()})
        cf_mods = {p["id"]: p for p in http(f"{CF_API}/mods", data={"modIds": pids})["data"]} if pids else {}
        for fp, pk in fps.items():
            x = hits.get(fp)
            if x:
                p, f = cf_mods[x["id"]], x["file"]
                cf_cache[pk["sha512"]] = {"project": x["id"], "file": f["id"], "fileName": f["fileName"], "slug": p["slug"],
                                          "name": p["name"], "allowModDistribution": p.get("allowModDistribution"),
                                          "downloadUrl": f.get("downloadUrl")}
        cf_cache_path.write_text(json.dumps(cf_cache, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    for pk in todo:
        c = cf_cache.get(pk["sha512"])
        if c and c["allowModDistribution"] and c["downloadUrl"]:
            pk["name"] = c["name"]
            pk["license"] = ""
            pk["source"] = {"type": "curseforge", "url": c["downloadUrl"],
                            "page": f"https://www.curseforge.com/minecraft/texture-packs/{c['slug']}"}
    unresolved = [pk["file"] for pk in packs if "source" not in pk]
    if unresolved:
        raise SystemExit(f"No download source for these resource packs (list them as bundled or dropped): {unresolved}")
    mods.extend(packs)

    # extra files copied out of a downloaded jar
    extracts = []
    for x in sources.get("extract", []):
        with zipfile.ZipFile(MC / "mods" / x["from"]) as z:
            data = z.read(x["entry"])
        side = next(m["side"] for m in mods if m["file"] == x["from"])
        extracts.append({"from": x["from"], "entry": x["entry"], "to": x["to"], "sha256": sha(data, "sha256"),
                         "side": side, "why": x["why"]})

    neo_url = (f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE_VERSION}/"
               f"neoforge-{NEOFORGE_VERSION}-installer.jar")
    neo_sha1 = http(neo_url + ".sha1", raw=True).decode().strip().split()[0]

    if args.check_urls:
        bad = []
        for m in mods:
            url = m["source"].get("url")
            if not url:
                continue
            try:
                # a plain GET, closed after the headers: CurseForge's CDN answers a ranged request with 404
                req = urllib.request.Request(url, headers={"User-Agent": UA})
                with urllib.request.urlopen(req, timeout=60) as r:
                    total = r.headers.get("Content-Length")
                    if total and total.isdigit() and int(total) != m["size"]:
                        bad.append(f"{m['file']}: size {total}, expected {m['size']}")
            except urllib.error.HTTPError as e:
                bad.append(f"{m['file']}: HTTP {e.code} {url}")
        print("link check:", "all answer" if not bad else f"{len(bad)} problem(s)")
        for b in bad:
            print("  ", b)

    out = {
        "format": 1,
        "pack": {"name": sources["pack"]["name"], "version": args.version, "repo": sources["pack"]["repo"],
                 "minecraft": "1.21.1"},
        "neoforge": {"version": NEOFORGE_VERSION, "installer": neo_url, "sha1": neo_sha1},
        "mods": [{k: v for k, v in m.items() if not k.startswith("_")} for m in mods],
        "extract": extracts,
    }
    (PACK / "manifest.json").write_text(json.dumps(out, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    counts = {}
    for m in mods:
        counts[m["source"]["type"]] = counts.get(m["source"]["type"], 0) + 1
    print("sources:", ", ".join(f"{k} {v}" for k, v in sorted(counts.items())),
          f"| patched {sum('patch' in m for m in mods)} | extracts {len(extracts)}")
    print("wrote", PACK / "manifest.json")


if __name__ == "__main__":
    main()
