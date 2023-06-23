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
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, EventRegistrar {
    private PSPLogger logger;
    private GeyserPerServerPack plugin;
    private HashMap<String, ServerInfo> playerCache;

    // needed as a temporary workaround until i can figure out how to always apply the correct pack.
    private ArrayList<String> xuidsWithDefaultPack;

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
        xuidsWithDefaultPack = new ArrayList<>();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);

        getLogger().info("GeyserPerServerPacks has been enabled!");

        if (config.isDebug()) {
            for (ServerInfo server : getProxy().getServers().values()) {
                for (ResourcePack pack : plugin.getPacks(server.getName())) {
                    logger.debug("Server " + server.getName() + " has pack " + pack.manifest().header().description());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnectEvent(ServerConnectEvent event) {
        logger.debug("ServerConnectEvent");

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
            logger.debug("contains xuid");
            if (playerCache.get(xuid) == null) {
                logger.error("Player " + xuid + " is known to us, but the server is null");
                return;
            }
            // case: player is known to us, redirecting
            logger.debug("Player " + xuid + " is known to us, redirecting to " + playerCache.get(xuid).getName());
            event.setTarget(playerCache.remove(xuid));
            playerCache.remove(xuid);
            return;
        } else {
            logger.debug("does not contain xuid");

            if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY || event.getReason() == ServerConnectEvent.Reason.LOBBY_FALLBACK) {
                if (plugin.getConfig().isKickOnMismatch()) {
                    if (xuidsWithDefaultPack.remove(xuid)) {
                        if (event.getTarget().getName().equals(plugin.getConfig().getDefaultServer())) {
                            logger.debug("Player " + xuid + " has the default pack, allowing connection");
                        } else {
                            logger.debug("Player " + xuid + " has the default pack, but is connecting to " + event.getTarget().getName() + ", kicking");
                            event.getPlayer().disconnect(new TextComponent(plugin.getConfig().getKickMessage()));
                            playerCache.put(xuid, event.getTarget());
                        }
                        return;
                    }
                }

                // TODO: use seamless transfer packet.
                // we need to get the target server here, save it, and reconnect bedrock player.
                // we cant just reconnect the player, since that is seemingly ignored while logging in

                //if (connection.transfer("127.0.0.1", 19132)) {
                //    this.getProxy().getScheduler().schedule(this, () -> {
                //        logger.info("Transfer sent");
                //        event.getPlayer().disconnect(new TextComponent("You have been redirected to the Bedrock server"));
                //    }, 55, TimeUnit.MILLISECONDS);
                //} else {
                //    logger.error("Failed to transfer player " + xuid + " to Bedrock server");
                //}
                return;
            }

            logger.debug("Player " + xuid + " is not known to us, saving " + event.getTarget().getName() + " as target server");
            playerCache.put(xuid, event.getTarget());
            connection.transfer(plugin.getConfig().getAddress(), plugin.getConfig().getPort());
            event.setCancelled(true);
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
            if (plugin.getConfig().isKickOnMismatch()) {
                xuidsWithDefaultPack.add(xuid);
            }
        }
    }
}
