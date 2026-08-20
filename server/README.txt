CrazyCraft 5.0 - Dedicated Server
=================================

QUICK START
1. Install Java 21 (https://adoptium.net/temurin/releases/?version=21)
2. Windows: double-click start.bat
   Linux/macOS: chmod +x start.sh && ./start.sh
3. First run installs NeoForge automatically (needs internet) and asks you
   to accept the Minecraft EULA - type "yes".
4. Done. The server runs on port 25565.

RAM
The start script uses 6 GB by default. To change it, edit the RAM= line at
the top of start.bat / start.sh. 6 GB minimum, 8 GB recommended.

WHAT'S DIFFERENT FROM THE CLIENT PACK
Client-only mods (shaders, HUD, menus, sounds, particles) are removed -
the full list is in REMOVED_CLIENT_MODS.txt. Everything gameplay-related
is identical, so client players connect with the normal client pack.

PORT FORWARDING
To let friends outside your network join, forward TCP port 25565 to this
machine, or use a tunneling service (playit.gg, ngrok).

BACKUPS
Your world lives in the "world" folder. Copy it somewhere safe regularly.
