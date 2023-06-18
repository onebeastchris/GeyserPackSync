package net.onebeastchris.geyserperserverpacks;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
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

    private Map<UUID, ServerInfo> playerCache;

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

        playerCache = new HashMap<>();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);

        getLogger().info("GeyserPerServerPacks has been enabled!");

        for (String servername : this.getProxy().getServers().keySet()) {
            getLogger().info("Server: " + servername);
        }
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        // check if the player is a bedrock player
        if (GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();

        // check if the player is already known to us - if it isn't, save server they want to connect to & reconnect for pack application
        if (playerCache.get(playerUUID) != null) {
            // The player is known to us, so we can just send them to the server they tried to connect to before
            event.setTarget(playerCache.get(playerUUID));
            playerCache.remove(playerUUID);
        } else {
            // The player is not known to us, so we need to save the server they tried to connect to and reconnect them to apply packs
            playerCache.put(playerUUID, event.getTarget());
            event.setCancelled(true);
            GeyserApi.api().transfer(playerUUID, address, port);
        }

    }


    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserSessionInit(SessionInitializeEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserConnection connection = event.connection();
        logger.info("LOGIN EVENT");
        // TODO: can we use forced hosts to get us the server name, so we can directly send the right packs?
        GeyserSession session = (GeyserSession) connection;
        session.remoteServer().address();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        logger.info("RESOURCE PACK REQUEST EVENT");
        UUID uuid = event.connection().javaUuid();

        if (!playerCache.containsKey(uuid)) return; // alterative: send hub/lobby packs here?
        String server = playerCache.get(uuid).getName();
        if (server != null) {
            List<ResourcePack> packs = plugin.getPacks(server);
            for (ResourcePack pack : packs) {
                event.register(pack);
            }
        }
        // If we do not know what server the player is headed to... optionally send hub/lobby packs here?

    }
}
