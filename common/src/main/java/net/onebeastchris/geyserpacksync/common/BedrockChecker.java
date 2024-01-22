package net.onebeastchris.geyserpacksync.common;

import io.netty.channel.Channel;
import net.onebeastchris.geyserpacksync.common.utils.FloodgateUtil;
import org.geysermc.api.connection.Connection;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.UUID;

public class BedrockChecker {
    private final boolean floodgate;
    private final GeyserApi geyserApi;

    public BedrockChecker(boolean floodgate) {
        this.floodgate = floodgate;
        this.geyserApi = GeyserApi.api();
    }

    public String getBedrockXuid(Channel channel, UUID uuid) {
        if (floodgate && channel != null) {
            try {
                GeyserPackSync.getLogger().debug("Looking up player " + uuid + " via channel hack");
                return FloodgateUtil.getBedrockXuid(channel);
            } catch (NoClassDefFoundError ignored) {
                return null;
            }
        } else {
            GeyserPackSync.getLogger().debug("Looking up player " + uuid + " via Geyser API");
            Connection connection = null;

            // loop through all geyser connections and hope we find it
            for (GeyserConnection geyserConnection : geyserApi.onlineConnections()) {
                if (geyserConnection.javaUuid().equals(uuid)) {
                    connection = geyserConnection;
                    break;
                }
            }

            return connection == null ? null : connection.xuid();
        }
    }
}
