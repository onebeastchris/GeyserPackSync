# GeyserPackSync

A Velocity/BungeeCord plugin that utilizes Geyser's API to allow for per-server resource packs. 

Example:
Lobby server with lobby resource pack, minigame server with minigame specific resource pack, etc.

#### Download: https://modrinth.com/plugin/geyserpacksync

### Current limitations
- Does not fully work with forced hosts/the player logging back in to a server that is not the default server.

### Installation
1. Download the latest release from the modrinth page/releases tab
2. Place the plugin in your Velocity/BungeeCord plugins folder
3. Restart your proxy
4. Configure the config file, specifically ip/port, and the servers. See the config file for more info!
5. Run `/reloadpacks` to reload the config, or restart the proxy.
6. Put the Bedrock edition resource packs in the folder with the name of the server they should be used on.
7. Restart the proxy, or run `/reloadpacks` to reload the config.

### Commands
- /reloadpacks - Reloads the config file and packs. Requires the `geyserpacksync.reload` permission.
- /packsyncreload - Alias for /reloadpacks; same permission.

For help with this project: https://discord.gg/WdmrRHRJhS

### DISCLAIMER: While this project is made to work with Geyser (literally requires geyser), it is not an official one - for help, ask in issues here or on the linked discord.

### How does it work?
Bedrock edition is... weird. It only allows resource packs to be sent once - before actually logging in to a server.
We also cannot disable/enable resource packs on the fly.

To work around this, this plugin sends the default resource pack(s) on login, and then sends the server specific resource pack(s) on server switch.
This means that the player will have to log out and back in to get the new resource pack(s) - which is done automatically with a transfer packet.

