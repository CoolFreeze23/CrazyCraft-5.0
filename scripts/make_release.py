"""Create the v1.0.0 GitHub release and upload the pack zips. Usage: make_release.py <token>"""
import json
import sys
import urllib.request
from pathlib import Path

token = sys.argv[1]
REPO = "CoolFreeze23/CrazyCraft-5.0"
BUILD = Path(r"C:\Homework\Projects\CrazyCraft5\build")

BODY = """The first packaged release of CrazyCraft 5.0.

## Downloads
- **CrazyCraft5-Client.zip** - full client pack. Import directly into Prism Launcher / MultiMC (Add Instance -> Import). Allocate 8-12 GB RAM.
- **CrazyCraft5-Server.zip** - ready-to-run dedicated server. Unzip, run `start.bat` (Windows) or `start.sh` (Linux/macOS), accept the EULA, play. Boot-tested headless before upload.

## Contents
- Minecraft 1.21.1, NeoForge 21.1.248, 193 mods (157 on the server)
- OreSpawn 2.0.0-beta.3 (faithful port of the 1.7.10 original), OreSpawn Integrations 0.4.3, OreSpawn Delight 0.1.0
- Complete Brazilian Portuguese localization (enabled by default; disable the resource pack for English)
- Six crash/compat-patched mods and curated configs - full details in the repo docs
"""


def api(url, data=None, method="POST", content_type="application/json"):
    req = urllib.request.Request(url, data=data, method=method, headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": content_type,
    })
    with urllib.request.urlopen(req) as r:
        return json.load(r)


rel = api(f"https://api.github.com/repos/{REPO}/releases", json.dumps({
    "tag_name": "v1.0.0",
    "target_commitish": "master",
    "name": "CrazyCraft 5.0 v1.0.0",
    "body": BODY,
    "prerelease": False,
}).encode())
print("release:", rel["html_url"])

for name in ("CrazyCraft5-Client.zip", "CrazyCraft5-Server.zip"):
    path = BUILD / name
    print(f"uploading {name} ({path.stat().st_size/1048576:.0f} MB)...", flush=True)
    url = rel["upload_url"].split("{")[0] + f"?name={name}"
    asset = api(url, path.read_bytes(), content_type="application/zip")
    print("  done:", asset["browser_download_url"], flush=True)

print("ALL_UPLOADS_COMPLETE")
