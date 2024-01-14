package net.onebeastchris.geyserpacksync.common;

import net.onebeastchris.geyserpacksync.common.utils.BackendServer;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncLogger;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncConfig;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ResourcePackLoader {
    public static HashMap<String, List<ResourcePack>> loadPacks(GeyserPackSync plugin) {
        PackSyncLogger logger = GeyserPackSync.getLogger();

        HashMap<String, List<ResourcePack>> serverPacks = new HashMap<>();

        if (plugin.getConfig() == null || plugin.getConfig().getServers() == null) {
            logger.warning("Config or servers null!");
            return serverPacks;
        }

        for (PackSyncConfig.Server server : plugin.getConfig().getServers()) {
            String serverName = server.name();
            Path serverPath = plugin.getDataFolder().resolve(serverName);
            if (!serverPath.toFile().exists()) {
                if (serverPath.toFile().mkdirs()) {
                    logger.info("Created folder for server " + serverName);
                    logger.info("You can now add Bedrock resource packs for " + serverName + " by adding them in the folder with that name.");
                }
            } else {
                logger.info("Found folder for server " + serverName);

                BackendServer backendServer = plugin.backendFromName(serverName);
                if (backendServer == null) {
                    logger.warning("Cannot find backend server with the name " + serverName + ". Skipping pack folder reading... ");
                    continue;
                }
                serverPacks.put(serverName, loadFromFolder(serverPath, logger));
            }
        }
        if (!serverPacks.isEmpty()) {
            logger.info("Loaded " + serverPacks.size() + " different server pack folders.");
        } else {
            logger.info("No pack folders were loaded. If this is your first time using this plugin, see the config file for instructions on usage.");
        }
        return serverPacks;
    }

    public static List<ResourcePack> loadFromFolder(Path path, PackSyncLogger logger) {
        List<ResourcePack> packs = new ArrayList<>();

        for (File file : Objects.requireNonNull(path.toFile().listFiles())) {
            try {
                ResourcePack pack = ResourcePack.create(PackCodec.path(file.toPath()));
                logger.debug("Adding pack: " + pack.manifest().header().name());
                packs.add(pack);
            } catch (Exception e) {
                logger.error("Failed to load pack " + file.getName() + " due to: " + e.getMessage());
                if (logger.isDebug()) e.printStackTrace();
            }
        }
        return packs;
    }
}
