#!/usr/bin/env bash
# Launch-safety check for Linux and SteamOS. Under a plain C locale (what Steam's Gaming Mode gives a launcher), a pack
# file whose name has § codes and accents must not stop `--client`, and switching between C and UTF-8 launches must
# never leave two copies of one pack.
#   bash test_locale.sh <java> <crazycraft-loader.jar>
# 1. v1.4.x's §-named entry under C: saved under the plain name, reported, exit 0.
# 2. The same entry under UTF-8: saved under its real name (the duplicate v1.5.0 fixes).
# 3. v1.5.0's ASCII-named entry under C, then under UTF-8: both old copies move to mods-removed/, exactly one remains.
# A player's own oddly named pack is never touched.
set -euo pipefail
JAVA="$1"; JAR="$2"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
export LC_ALL=C.UTF-8 LANG=C.UTF-8   # the fixture itself is written with UTF-8 names
G="$T/game"; mkdir -p "$G/manual" "$G/resourcepacks" "$G/mods"
printf 'PK\005\006%018d' 0 > "$G/manual/redstone-torch-download.zip"     # an empty zip
printf 'PK\005\006%018d' 1 > "$G/resourcepacks/§eMeu Pacote São João.zip"  # a player's own pack
SIZE=$(stat -c %s "$G/manual/redstone-torch-download.zip")
SHA=$(sha512sum "$G/manual/redstone-torch-download.zip" | cut -d' ' -f1)
manifest() {  # $1 = version, $2 = the pack's file name
cat > "$T/manifest-$1.json" <<EOF
{"format": 1,
 "pack": {"name": "CrazyCraft 5.0", "version": "$1", "repo": "CoolFreeze23/CrazyCraft-5.0", "minecraft": "1.21.1"},
 "neoforge": {"version": "21.1.252", "installer": "https://example.invalid/neoforge.jar", "sha1": "0"},
 "mods": [{"file": "$2", "dir": "resourcepacks", "name": "Redstone Torch", "version": "1",
           "side": "client", "size": $SIZE, "sha512": "$SHA",
           "source": {"type": "manual", "page": "https://example.invalid/redstone-torch"}, "license": "MIT"}],
 "extract": []}
EOF
}
manifest old "§c1.21.2 Redstone Torch§r.zip"
manifest new "Redstone Torch Backport 1.21.2.zip"
fail() { echo "FAIL: $1"; ls -la "$G/resourcepacks"; exit 1; }
run() {  # $1 = locale, $2 = manifest version
    set +e
    LC_ALL="$1" LANG="$1" "$JAVA" -Djava.awt.headless=true -jar "$JAR" --client --nogui --dir "$G" \
        --manifest "$T/manifest-$2.json" > "$T/out.txt" 2>&1
    local code=$?
    set -e
    sed 's/^/    /' "$T/out.txt"
    [ "$code" -eq 0 ] || fail "loader exited $code ($1, $2 manifest)"
}
torches() { find "$G/resourcepacks" -maxdepth 1 -name '*Redstone Torch*' | wc -l; }
echo "sun.jnu.encoding under LC_ALL=C: $(LC_ALL=C "$JAVA" -XshowSettings:properties -version 2>&1 | grep -m1 'sun.jnu.encoding' | tr -d ' ')"

echo "1. old manifest, C locale"; run C old
[ -f "$G/resourcepacks/1.21.2 Redstone Torch.zip" ] || fail "the plain-name copy was not placed"
grep -q "1.21.2 Redstone Torch.zip" "$T/out.txt" || fail "the plain-name fallback was not reported"
echo "2. old manifest, UTF-8"; run C.UTF-8 old
[ "$(torches)" -eq 2 ] || fail "expected the two copies v1.4.x leaves, found $(torches)"
echo "3a. new manifest, C locale"; run C new
[ "$(torches)" -eq 1 ] || fail "expected one copy after the update under C, found $(torches)"
[ -f "$G/resourcepacks/Redstone Torch Backport 1.21.2.zip" ] || fail "the new plain-named pack is missing"
echo "3b. new manifest, UTF-8"; run C.UTF-8 new
[ "$(torches)" -eq 1 ] || fail "expected one copy after a UTF-8 launch, found $(torches)"
[ -f "$G/resourcepacks/§eMeu Pacote São João.zip" ] || fail "the player's own pack was touched"
[ -n "$(find "$G/mods-removed" -name '*Redstone Torch*' 2>/dev/null)" ] || fail "the old copies were not kept in mods-removed/"
echo "PASS: no launch fails on special characters, and C and UTF-8 launches keep exactly one copy"
