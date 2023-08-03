package net.onebeastchris.geyserpacksync.common;
import net.onebeastchris.geyserpacksync.common.utils.Configurate;
import net.onebeastchris.geyserpacksync.common.utils.ResourcePackLoader;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GeyserPackSync {
    private final Path dataFolder;
    private final Configurate config;
    private final PSPLogger logger;
    private final Map<String, List<ResourcePack>> packs;

    public GeyserPackSync(Path dataFolder, Configurate config, PSPLogger logger) {
        this.dataFolder = dataFolder;
        this.config = config;
        this.logger = logger;

        this.packs = ResourcePackLoader.loadPacks(this);
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
        var list = this.packs.get(server);
        if (list == null) {
            logger.debug("No packs found for server " + server);
            return Collections.emptyList();
        }
        return list;
    }
}
