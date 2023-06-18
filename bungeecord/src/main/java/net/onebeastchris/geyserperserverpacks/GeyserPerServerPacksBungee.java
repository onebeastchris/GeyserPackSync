package net.onebeastchris.geyserperserverpacks;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import org.geysermc.api.connection.Connection;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, EventRegistrar {
    private PSPLogger logger;
    private GeyserPerServerPack plugin;
    private int port;
    private String address;

    private Map<String, ServerInfo> playerCache;

    @Override
    public void onEnable() {
        Configurate config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        logger = new LoggerImpl(this.getLogger());

        if (!hasGeyser) {
            getLogger().warning("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        plugin = new GeyserPerServerPack(this.getDataFolder().toPath(), config, logger);

        port = GeyserApi.api().bedrockListener().port();
        String configAddress = GeyserApi.api().bedrockListener().address();
        address = configAddress.equals("0.0.0.0") ? GeyserApi.api().defaultRemoteServer().address() : configAddress;

        logger.info(address);
        logger.info(configAddress);
        logger.info(GeyserApi.api().defaultRemoteServer().address());

        playerCache = new HashMap<>();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);

        getLogger().info("GeyserPerServerPacks has been enabled!");
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        // check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();
        Connection connection = GeyserApi.api().connectionByUuid(playerUUID);
        if (connection == null) {
            logger.error("Connection is null for Bedrock player " + playerUUID);
            return;
        }

        String xuid = connection.xuid();

        // check if the player is already known to us - if it isn't, save server they want to connect to & reconnect for pack application
        if (playerCache.get(xuid) != null) {

            logger.info("Player known - connecting to server: " + playerCache.get(xuid).getName());
            // The player is known to us, so we can just send them to the server they tried to connect to before
            event.setTarget(playerCache.get(xuid));
            playerCache.remove(xuid);

        } else {
            // The player is not known to us, so we need to save the server they tried to connect to and reconnect them to apply packs
            playerCache.put(xuid, event.getTarget());
            logger.info("Player unknown - saving server: " + event.getTarget().getName());
            GeyserApi.api().transfer(playerUUID, address, port);
            // do we need to cancel the event?
        }

    }


    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserSessionInit(SessionInitializeEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserConnection connection = event.connection();
        // TODO: can we use forced hosts to get us the server name, so we can directly send the right packs?
        GeyserSession session = (GeyserSession) connection;
        session.remoteServer().address();
        logger.info("Server: " + session.remoteServer().address());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();

        if (!playerCache.containsKey(xuid)) {
            List<ResourcePack> packs = plugin.getPacks("lobby");
            for (ResourcePack pack : packs) {
                event.register(pack);
            }
        } else {
            String server = playerCache.get(xuid).getName();
            if (server != null) {
                List<ResourcePack> packs = plugin.getPacks(server);
                for (ResourcePack pack : packs) {
                    event.register(pack);
                }
            }
        }
    }
}
