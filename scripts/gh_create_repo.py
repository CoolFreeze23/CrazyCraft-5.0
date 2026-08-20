"""Create a GitHub repo via the REST API. Usage: gh_create_repo.py <token> <name> <description>"""
import json
import sys
import urllib.request

token, name, desc = sys.argv[1], sys.argv[2], sys.argv[3]
req = urllib.request.Request(
    "https://api.github.com/user/repos",
    data=json.dumps({"name": name, "description": desc, "private": False}).encode(),
    headers={"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json"},
    method="POST",
)
try:
    with urllib.request.urlopen(req) as r:
        d = json.load(r)
        print("created:", d["html_url"])
except urllib.error.HTTPError as e:
    body = e.read().decode()
    if "name already exists" in body:
        print("already exists")
    else:
        print("HTTP", e.code, body)
        sys.exit(1)
