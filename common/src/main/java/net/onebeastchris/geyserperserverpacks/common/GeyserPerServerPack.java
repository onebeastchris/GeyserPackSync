package net.onebeastchris.geyserperserverpacks.common;
import net.onebeastchris.geyserperserverpacks.common.utils.ResourcePackLoader;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GeyserPerServerPack {
    private final Path dataFolder;
    private final Configurate config;
    private final PSPLogger logger;
    private final Map<String, List<ResourcePack>> packs;

    public GeyserPerServerPack(Path dataFolder, Configurate config, PSPLogger logger) {
        this.dataFolder = dataFolder;
        this.config = config;
        this.logger = logger;
        this.packs = ResourcePackLoader.loadPacks(this);

        if (config.getPort() <= 0 || config.getPort() > 65535) {
            logger.error("Invalid port! Please set a valid port in the config!");
            return;
        }

        if (config.getAddress() == null || config.getAddress().isEmpty()) {
            logger.error("Invalid address! Please set a valid address in the config!");
        }
    }

    public Path getDataFolder() {
        return this.dataFolder;
    }

    public Configurate getConfig() {
        return this.config;
    }

    public PSPLogger getLogger() {
        return this.logger;
    }
    public List<ResourcePack> getPacks(String server) {
        return this.packs.get(server);
    }
}
