package net.onebeastchris.geyserpacksync;

import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;
import net.onebeastchris.geyserpacksync.common.GeyserPackSync;

import java.lang.reflect.Method;

public class VelocityAccessor {

    private static final Class<?> CONNECTED_PLAYER;
    private static final Method MINECRAFT_CONNECTION_GETTER;

    private static final Class<?> MINECRAFT_CONNECTION;

    private static final Method CHANNEL_GETTER;

    private static boolean successful;

    public static void init() {
    }

    static {
        try {
            CONNECTED_PLAYER = Class.forName("com.velocitypowered.proxy.connection.client.ConnectedPlayer");
            MINECRAFT_CONNECTION_GETTER = CONNECTED_PLAYER.getMethod("getConnection");

            MINECRAFT_CONNECTION = Class.forName("com.velocitypowered.proxy.connection.MinecraftConnection");
            CHANNEL_GETTER = MINECRAFT_CONNECTION.getMethod("getChannel");

            successful = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            successful = false;
            throw new IllegalStateException("Unable to hook into velocity internals! Not able to use channel lookup hack." +
                    "Please file an issue here: https://github.com/onebeastchris/GeyserPackSync");
        }
    }

    public static Channel getChannel(Player player) {
        Channel channel = null;

        if (successful) {
            try {
                Object connectedPlayer = CONNECTED_PLAYER.cast(player);
                Object connection = MINECRAFT_CONNECTION_GETTER.invoke(connectedPlayer);

                channel = (Channel) CHANNEL_GETTER.invoke(connection);
            } catch (Exception e) {
                GeyserPackSync.getLogger().error("Unable to get channel: " + e.getMessage());
                return null;
            }
        }

        return channel;
    }
}
