package net.onebeastchris.geyserperserverpacks.common;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface GeyserPerServerPackBootstrap {
    Path dataFolder();
    Configurate config();
    PSPLogger logger();
    Map<String, List<ResourcePack>> packs();
}
