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
    private Map<String, ServerInfo> playerCache;

    @Override
    public void onEnable() {
        Configurate config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        logger = new LoggerImpl(this.getLogger());

        logger.setDebug(config.isDebug());

        if (!hasGeyser) {
            getLogger().severe("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        plugin = new GeyserPerServerPack(this.getDataFolder().toPath(), config, logger);

        playerCache = new HashMap<>();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);

        getLogger().info("GeyserPerServerPacks has been enabled!");

        for (ServerInfo server : getProxy().getServers().values()) {
            for (ResourcePack pack : plugin.getPacks(server.getName())) {
                logger.debug("Server " + server.getName() + " has pack " + pack.manifest().header().description());
            }
        }

    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        // we cannot use our magic when the player does not have a "current" server - we use fallback packs in this case.
        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }

        // check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            logger.debug("Player " + event.getPlayer().getName() + " is not a Bedrock player");
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();
        Connection connection = GeyserApi.api().connectionByUuid(playerUUID);
        if (connection == null) {
            logger.error("Connection is null for Bedrock player " + playerUUID);
            return;
        }

        String xuid = connection.xuid();
        if (playerCache.containsKey(xuid)) {
            // case: player is known to us, redirecting
            logger.debug("Player " + xuid + " is known to us, redirecting to " + playerCache.get(xuid).getName());
            event.setTarget(playerCache.remove(xuid));
        } else {
            playerCache.put(xuid, event.getTarget());
            logger.debug("Player " + xuid + " is not known to us, so we do not redirect yet");
            connection.transfer(plugin.getConfig().getAddress(), plugin.getConfig().getPort());
            event.setCancelled(true);

            // TODO: get forced hosts to improve our guessing game
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onSessionInitialize(SessionInitializeEvent event) {
        GeyserSession session = (GeyserSession) event.connection();

        // TODO: grab forced hosts to improve our guessing game
        logger.info(session.remoteServer().address() == null ? "null" : session.remoteServer().address());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();

        if (playerCache.containsKey(xuid)) {
            String server = playerCache.get(xuid).getName();
            logger.debug("Player " + xuid + " is known to us, so we send server packs for " + server);
            if (server != null) {
                List<ResourcePack> packs = plugin.getPacks(server);
                for (ResourcePack pack : packs) {
                    event.register(pack);
                }
            }
        } else {
            logger.debug("Player " + xuid + " is not known to us, so we send default packs");
            List<ResourcePack> packs = plugin.getPacks(plugin.getConfig().getDefaultServer());
            for (ResourcePack pack : packs) {
                event.register(pack);
            }
        }
    }
}
