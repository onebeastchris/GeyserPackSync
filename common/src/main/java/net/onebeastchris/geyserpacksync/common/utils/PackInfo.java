package net.onebeastchris.geyserpacksync.common.utils;

import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.util.List;
import java.util.UUID;

public record PackInfo(
        String name,
        UUID uuid,
        List<BedrockPlatform> blockedPlatforms,
        List<InputMode> blockedInputModes,
        ResourcePack pack
) {
    public boolean isCompatible(GeyserConnection connection) {

        if (blockedPlatforms.contains(connection.platform())) {
            return false;
        }

        if (blockedInputModes.contains(connection.inputMode())) {
            return false;
        }

        return true;
    }


}
