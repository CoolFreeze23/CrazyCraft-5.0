CrazyCraft 5.0 - Dedicated Server
=================================

These server files don't include any mods. The first time you set up the server, it
downloads every mod from its official page (Modrinth, CurseForge or the author's own
GitHub) and checks each file before using it. That's how the pack respects the people
who made the mods: their files only ever come from them.

QUICK START (WINDOWS)
1. Install Java 21: https://adoptium.net/temurin/releases/?version=21
2. Double-click "CrazyCraft Server Setup.bat". The setup window installs NeoForge and
   the mods, asks how much memory the server gets and about the Minecraft EULA, and
   then starts the server for you.
3. From then on, start the server with startserver.bat.

QUICK START (LINUX, MACOS OR A SERVER WITHOUT A SCREEN)
1. Install Java 21.
2. Run:  chmod +x startserver.sh && ./startserver.sh
   It sets everything up in the terminal, asks about the EULA, and starts the server.
   To answer the EULA question ahead of time: CRAZYCRAFT_ACCEPT_EULA=true ./startserver.sh

ONE MOD BY HAND: MCHELI
MCHeli's author only allows downloads from CurseForge itself, so setup can't fetch it.
Setup shows you the download page; save the file into the "manual" folder here or into
your Downloads folder, and setup picks it up and checks it.

EVERY START CHECKS THE MODS
startserver.bat / startserver.sh run the setup in the console before each start. It
takes a few seconds when nothing changed, fetches anything missing, and then starts the
server. If the server stops or crashes, it restarts after 10 seconds (Ctrl+C cancels).
Settings, as environment variables:
  CRAZYCRAFT_JAVA=<path to java>   use a specific Java 21
  CRAZYCRAFT_RESTART=false         don't restart after a stop
  CRAZYCRAFT_INSTALL_ONLY=true     set up or update, then exit
  CRAZYCRAFT_SKIP_SETUP=true       start without checking the mods
  CRAZYCRAFT_ACCEPT_EULA=true      accept the Minecraft EULA without asking

MEMORY
Edit -Xmx in user_jvm_args.txt (or use the setup window). 6 GB minimum, 8 GB is better.

UPDATING TO A NEW PACK VERSION
Download the new server files and unzip them over this folder, then start as usual.
Setup downloads only what changed and moves jars that are no longer part of the pack
to mods-removed/. Your world, server.properties and whitelist are left alone.

YOUR OWN EXTRA MODS
Put them in mods/ and list their file names in user-mods.txt, so setup leaves them be.

PLAYERS
Players join with the CrazyCraft 5.0 client of the same version. Client-only mods
(shaders, HUD, menus, sounds, particles) aren't installed on the server.

PORT FORWARDING
To let friends outside your network join, forward TCP port 25565 to this machine, or
use a tunneling service (playit.gg, ngrok).

BACKUPS
Your world lives in the "world" folder. Copy it somewhere safe regularly.

CREDITS
CREDITS.txt lists every mod, where it comes from and, for the few the pack fixes, what
the fix is. The setup window has the same list under "Mods & credits".
