package net.onebeastchris.geyserpacksync.common;

import net.onebeastchris.geyserpacksync.common.utils.PackSyncLogger;
import net.onebeastchris.geyserpacksync.common.utils.BackendServer;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncConfig;

import java.nio.file.Path;

public interface GeyserPackSyncBootstrap {
    PackSyncLogger logger();

    BackendServer backendFromName(String name);

    Path dataFolder();

    PackSyncConfig config();

    boolean floodgatePresent();
}
