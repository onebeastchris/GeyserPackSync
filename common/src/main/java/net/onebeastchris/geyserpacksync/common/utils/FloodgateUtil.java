package net.onebeastchris.geyserpacksync.common.utils;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

// separate class to prevent classnotfoundexceptions :p
public class FloodgateUtil {

    private static final AttributeKey<FloodgatePlayer> floodgate_player = AttributeKey.valueOf("floodgate-player");

    public static String getBedrockXuid(Channel channel) {
        FloodgatePlayer player = channel.attr(floodgate_player).get();
        return player == null ? null : player.getXuid();
    }
}
