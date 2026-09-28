#!/bin/sh
# CrazyCraft 5.0 server: checks the mods, then starts the server and restarts it when it stops.
#   CRAZYCRAFT_JAVA          full path to java, if "java" is not Java 21
#   CRAZYCRAFT_RESTART=false  stop instead of restarting when the server stops or crashes
#   CRAZYCRAFT_INSTALL_ONLY=true  install or update the server, then exit
#   CRAZYCRAFT_SKIP_SETUP=true    start without checking the mods first
#   CRAZYCRAFT_ACCEPT_EULA=true   accept the Minecraft EULA (https://aka.ms/MinecraftEULA) without asking
set -u
NEOFORGE_VERSION=21.1.248
JAVA="${CRAZYCRAFT_JAVA:-java}"
cd "$(dirname "$0")"

if ! command -v "$JAVA" >/dev/null 2>&1; then
    echo "CrazyCraft 5.0 needs Java 21, and Java was not found."
    echo "Get it from https://adoptium.net/temurin/releases/?version=21"
    exit 1
fi
JAVA_MAJOR=$("$JAVA" -fullversion 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
    echo "CrazyCraft 5.0 needs Java 21 or newer, but this is Java ${JAVA_MAJOR}."
    exit 1
fi

if [ "${CRAZYCRAFT_SKIP_SETUP:-false}" != "true" ]; then
    "$JAVA" -jar crazycraft-loader.jar --nogui
    SETUP=$?
    case "$SETUP" in
        0) ;;
        2)
            [ "${CRAZYCRAFT_INSTALL_ONLY:-false}" = "true" ] && exit 0
            echo "The server can't start until the Minecraft EULA is accepted."
            exit 2 ;;
        3)
            echo "A mod still has to be downloaded by hand; see above. Then run this again."
            exit 3 ;;
        *)
            echo "Setup did not finish; see above."
            exit 1 ;;
    esac
fi
[ "${CRAZYCRAFT_INSTALL_ONLY:-false}" = "true" ] && exit 0

while true; do
    echo "Starting the CrazyCraft 5.0 server..."
    "$JAVA" @user_jvm_args.txt @libraries/net/neoforged/neoforge/$NEOFORGE_VERSION/unix_args.txt nogui
    [ "${CRAZYCRAFT_RESTART:-true}" = "false" ] && exit 0
    echo "The server stopped. Restarting in 10 seconds (press Ctrl+C to cancel)."
    sleep 10
done
