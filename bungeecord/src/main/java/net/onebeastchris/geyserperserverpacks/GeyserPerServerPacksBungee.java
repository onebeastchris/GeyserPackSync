package net.onebeastchris.geyserperserverpacks;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.event.EventPriority;
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
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, EventRegistrar {
    private PSPLogger logger;
    private GeyserPerServerPack plugin;
    private HashMap<String, ServerInfo> playerCache;
    private HashMap<UUID, String> tempUntilServerKnown;

    @Override
    public void onEnable() {
        Configurate config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        if (!hasGeyser) {
            getLogger().severe("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        if (config == null) {
            logger.error("There was an error loading the config!");
            return;
        }

        logger = new LoggerImpl(this.getLogger());
        plugin = new GeyserPerServerPack(this.getDataFolder().toPath(), config, logger);
        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);

        getLogger().info("GeyserPerServerPacks has been enabled!");
        logger.setDebug(config.isDebug());

        logger.debug("Debug mode is enabled");
        if (config.isDebug()) {
            for (ServerInfo server : getProxy().getServers().values()) {
                logger.debug("Server: " + server.getName());
                logger.debug("Packs: " + plugin.getPacks(server.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnectEvent(ServerConnectEvent event) {
        logger.debug("ServerConnectEvent - lowest priority");
        UUID uuid = event.getPlayer().getUniqueId();

        //Check if the player is a bedrock player
        if (!tempUntilServerKnown.containsKey(uuid)) {
            logger.debug("No need to grab server!");
            return;
        }

        if (!event.getTarget().canAccess(event.getPlayer())) {
            logger.debug("Player " + event.getPlayer().getName() + " can't access " + event.getTarget().getName());
            tempUntilServerKnown.remove(uuid);
            return;
        }

        String xuid = tempUntilServerKnown.remove(uuid);
        ServerInfo server = event.getTarget();

        playerCache.put(xuid, server);

        boolean firstConnection = event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY || event.getReason() == ServerConnectEvent.Reason.LOBBY_FALLBACK;
        // ugly, yes, but until we use void/limbo servers, this is the only way :(
        if (firstConnection) {
            if (server.getName().equals(plugin.getConfig().getDefaultServer())) {
                logger.debug("Player " + xuid + " has the default pack, and is going to the default server, allowing connection");
                playerCache.remove(xuid);
            } else {
                if (plugin.getConfig().isKickOnMismatch()) {
                    logger.debug("Player " + xuid + " has the default pack, but is connecting to " + server.getName() + ", kicking");
                    event.getPlayer().disconnect(new TextComponent(plugin.getConfig().getKickMessage()));
                    event.setCancelled(true);
                } else {
                    logger.warning("Player " + xuid + " has the default pack, but is connecting to " + server.getName() + ".");
                }
            }
        } else {
            logger.debug("Player " + xuid + " is being reconnected...");
            GeyserApi.api().transfer(uuid, plugin.getConfig().getAddress(), plugin.getConfig().getPort());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConnect(ServerConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(uuid)) {
            logger.debug("Player " + event.getPlayer().getName() + " is not a Bedrock player");
            return;
        }

        Connection connection = GeyserApi.api().connectionByUuid(uuid);
        if (connection == null) {
            logger.error("Connection is null for Bedrock player " + uuid);
            return;
        }

        String xuid = connection.xuid();
        if (playerCache.containsKey(xuid)) {
            logger.debug("contains xuid");
            if (playerCache.get(xuid) == null) {
                logger.error("Player " + xuid + " is known to us, but the server is null");
                return;
            }
            // case: player is known to us, redirecting
            logger.debug("Player " + xuid + " is known to us, redirecting to " + playerCache.get(xuid).getName());
            event.setTarget(playerCache.remove(xuid));
            playerCache.remove(xuid);
        } else {
            logger.debug("does not contain xuid");
            // grab server once event is completed to not break compat with other plugins changing the destination server.
            tempUntilServerKnown.put(uuid, xuid);
        }
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
