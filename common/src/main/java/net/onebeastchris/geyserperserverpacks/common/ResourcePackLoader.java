package net.onebeastchris.geyserperserverpacks.common;

import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

public class ResourcePackLoader {

    private final PSPLogger logger;

    public ResourcePackLoader(PSPLogger logger) {
        this.logger = logger;
    }

    public HashMap<String, ResourcePack> loadFromFolder(Path path) {
        HashMap<String, ResourcePack> packs = new HashMap<>();
        for (File file : Objects.requireNonNull(path.toFile().listFiles())) {
            try {
                ResourcePack pack = ResourcePack.create(PackCodec.path(file.toPath()));
                String uuid = pack..header().uuid().toString();
                packs.put(uuid, pack);

            } catch (Exception e) {
                logger.error("Failed to load pack " + file.getName(), e);
                e.printStackTrace();
            }
        }
        return packs;
    }
}
