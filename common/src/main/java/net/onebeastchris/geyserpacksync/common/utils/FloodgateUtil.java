package net.onebeastchris.geyserpacksync.common.utils;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.geysermc.api.connection.Connection;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.UUID;

// separate class to prevent classnotfoundexceptions :p
public class FloodgateUtil {

    private static final AttributeKey<FloodgatePlayer> floodgate_player = AttributeKey.valueOf("floodgate-player");

    public static String getBedrockXuid(Channel channel, UUID uuid) {
        FloodgatePlayer player = channel.attr(floodgate_player).get();
        return player == null ? null : player.getXuid();
    }
}
