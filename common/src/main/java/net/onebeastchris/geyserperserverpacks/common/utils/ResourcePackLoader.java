package net.onebeastchris.geyserperserverpacks.common.utils;

import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ResourcePackLoader {
    public static HashMap<String, List<ResourcePack>> loadPacks(GeyserPerServerPack bootstrap) {
        PSPLogger logger = bootstrap.getLogger();

        HashMap<String, List<ResourcePack>> serverPacks = new HashMap<>();

        if (bootstrap.getConfig() == null || bootstrap.getConfig().getServers() == null) {
            return serverPacks;
        }

        for (Configurate.Server server : bootstrap.getConfig().getServers()) {
            if (!bootstrap.getDataFolder().resolve(server.name()).toFile().exists()) {
                if (bootstrap.getDataFolder().resolve(server.name()).toFile().mkdirs()) {
                    logger.info("Created folder for server " + server.name());
                    logger.info("You can now add Bedrock resource packs for " + server.name() + " by adding them in the folder with that name");
                }
            } else {
                logger.info("Found folder for server " + server.name());
                serverPacks.put(server.name(), loadFromFolder(bootstrap.getDataFolder().resolve(server.name()), logger));
            }
        }
        if (serverPacks.size() > 0) {
            logger.info("Loaded " + serverPacks.size() + " different server pack folders.");
        } else {
            logger.info("No pack folders were loaded. If this is your first time using this plugin, see the config file for instructions on usage.");
        }
        return serverPacks;
    }

    public static List<ResourcePack> loadFromFolder(Path path, PSPLogger logger) {
        List<ResourcePack> packs = new ArrayList<>();

        for (File file : Objects.requireNonNull(path.toFile().listFiles())) {
            try {
                ResourcePack pack = ResourcePack.create(PackCodec.path(file.toPath()));
                packs.add(pack);
            } catch (Exception e) {
                logger.error("Failed to load pack " + file.getName(), e);
                e.printStackTrace();
            }
        }
        return packs;
    }
}
