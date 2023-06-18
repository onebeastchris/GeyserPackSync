package net.onebeastchris.geyserperserverpacks.common;
import net.onebeastchris.geyserperserverpacks.common.utils.PlayerStorage;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeyserPerServerPack {
    private final Path dataFolder;
    private final Configurate config;
    private final PSPLogger logger;
    private Map<String, List<ResourcePack>> packs;

    public GeyserPerServerPack(Path dataFolder, Configurate config, PSPLogger logger) {
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

    public Map<String, List<ResourcePack>> getPacks() {
        return this.packs;
    }

    public List<ResourcePack> getPacks(String server) {
        return this.packs.get(server);
    }
}
