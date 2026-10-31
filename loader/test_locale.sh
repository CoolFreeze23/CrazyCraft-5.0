#!/usr/bin/env bash
# Launch-safety check for Linux and SteamOS: under a plain C locale (what Steam's Gaming Mode gives a launcher), a
# pack file whose name has § codes and accents must not stop `--client`. It is saved under a plain name, the log says
# so, a player's own oddly named pack is left alone, and the loader exits 0.
#   bash test_locale.sh <java> <crazycraft-loader.jar>
set -euo pipefail
JAVA="$1"; JAR="$2"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
export LC_ALL=C.UTF-8 LANG=C.UTF-8   # the fixture itself is written with UTF-8 names
G="$T/game"; mkdir -p "$G/manual" "$G/resourcepacks" "$G/mods"
printf 'PK\005\006%018d' 0 > "$G/manual/redstone-torch-download.zip"     # an empty zip
printf 'PK\005\006%018d' 1 > "$G/resourcepacks/§eMeu Pacote São João.zip"  # a player's own pack
SIZE=$(stat -c %s "$G/manual/redstone-torch-download.zip")
SHA=$(sha512sum "$G/manual/redstone-torch-download.zip" | cut -d' ' -f1)
cat > "$T/manifest.json" <<EOF
{"format": 1,
 "pack": {"name": "CrazyCraft 5.0", "version": "test", "repo": "CoolFreeze23/CrazyCraft-5.0", "minecraft": "1.21.1"},
 "neoforge": {"version": "21.1.252", "installer": "https://example.invalid/neoforge.jar", "sha1": "0"},
 "mods": [{"file": "§c1.21.2 Redstone Torch§r.zip", "dir": "resourcepacks", "name": "Redstone Torch", "version": "1",
           "side": "client", "size": $SIZE, "sha512": "$SHA",
           "source": {"type": "manual", "page": "https://example.invalid/redstone-torch"}, "license": "MIT"}],
 "extract": []}
EOF
set +e
LC_ALL=C LANG=C "$JAVA" -Djava.awt.headless=true -jar "$JAR" --client --nogui --dir "$G" --manifest "$T/manifest.json" > "$T/out.txt" 2>&1
CODE=$?
set -e
cat "$T/out.txt"
echo "sun.jnu.encoding under LC_ALL=C: $(LC_ALL=C "$JAVA" -XshowSettings:properties -version 2>&1 | grep -m1 'sun.jnu.encoding' | tr -d ' ')"
fail() { echo "FAIL: $1"; exit 1; }
[ "$CODE" -eq 0 ] || fail "loader exited $CODE"
[ -f "$G/resourcepacks/1.21.2 Redstone Torch.zip" ] || [ -f "$G/resourcepacks/§c1.21.2 Redstone Torch§r.zip" ] || fail "the pack file was not placed"
[ -f "$G/resourcepacks/§eMeu Pacote São João.zip" ] || fail "the player's own pack was touched"
if [ -f "$G/resourcepacks/1.21.2 Redstone Torch.zip" ]; then
    grep -q "1.21.2 Redstone Torch.zip" "$T/out.txt" "$G/.crazycraft/loader.log" || fail "the plain-name fallback was not reported"
fi
echo "PASS: launch survives special characters under LC_ALL=C (exit $CODE)"
