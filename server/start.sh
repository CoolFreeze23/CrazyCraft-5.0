#!/usr/bin/env bash
# CrazyCraft 5.0 server launcher
set -e
cd "$(dirname "$0")"

# ============ SETTINGS ============
RAM="${RAM:-6G}"           # 6G minimum, 8G recommended
NEOFORGE="21.1.248"
# ==================================

if ! command -v java >/dev/null 2>&1; then
    echo "[!] Java not found. Install Java 21: https://adoptium.net/temurin/releases/?version=21"
    exit 1
fi
JMAJOR=$(java -version 2>&1 | awk -F'"' '/version/ {split($2,a,"."); print a[1]}')
if [ "$JMAJOR" -lt 21 ]; then
    echo "[!] Java $JMAJOR found, but this server needs Java 21."
    exit 1
fi

if [ ! -f "libraries/net/neoforged/neoforge/${NEOFORGE}/unix_args.txt" ]; then
    echo "[*] First run - installing NeoForge ${NEOFORGE} (needs internet)..."
    java -jar "neoforge-${NEOFORGE}-installer.jar" --install-server
fi

if ! grep -q "eula=true" eula.txt 2>/dev/null; then
    echo
    echo "  To run a Minecraft server you must accept the Minecraft EULA:"
    echo "  https://aka.ms/MinecraftEULA"
    echo
    read -r -p "Do you accept the EULA? (yes/no): " ACCEPT
    if [ "$ACCEPT" = "yes" ] || [ "$ACCEPT" = "y" ]; then
        echo "eula=true" > eula.txt
    else
        echo "[!] EULA not accepted - can't start the server."
        exit 1
    fi
fi

echo "[*] Starting CrazyCraft 5.0 server with ${RAM} RAM..."
exec java -Xms2G -Xmx"${RAM}" -XX:+UseG1GC -XX:MaxGCPauseMillis=130 \
    @"libraries/net/neoforged/neoforge/${NEOFORGE}/unix_args.txt" nogui "$@"
