package net.onebeastchris.geyserperserverpacks.common;

import net.onebeastchris.geyserperserverpacks.common.utils.PlayerStorage;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public interface GeyserPerServerPackBootstrap {

    void onEnable();

    void onDisable();

    Configurate getConfig();

    ResourcePackLoader getResourcePackLoader();
    PSPLogger getGPSPLogger();

    Map<String, HashMap<String, ResourcePack>> serverPacks = new HashMap<>();
    Path packsDataFolders();

    PlayerStorage getPlayerStorage();

    default void loadPacks() {
        for (String server : getConfig().getServers()) {
            if (!packsDataFolders().resolve(server).toFile().exists()) {
                if (packsDataFolders().resolve(server).toFile().mkdirs()) {
                    getGPSPLogger().info("Created folder for server " + server);
                    getGPSPLogger().info("You can now add bedrock resource packs for " + server + " by adding them in the folder with that name");
                }
            } else {
                serverPacks.put(server, getResourcePackLoader().loadFromFolder(packsDataFolders().resolve(server)));
            }
        }
        if (serverPacks.size() > 0) {
            getGPSPLogger().info("Loaded " + serverPacks.size() + " different pack folders.");
        } else {
            getGPSPLogger().info("No pack folders were loaded. If this is your first time using this plugin, see the config file for instructions on usage.");
        }
    }

    default void reloadPacks() {
        serverPacks.clear();
        loadPacks();
    }

}
