package net.onebeastchris.geyserperserverpacks;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Plugin(id = "geyserperserverpacks", name = "GeyserPerServerPacks", version = "1.0-SNAPSHOT",
        url = "github.com/onebeastchris/GeyserPerServerPacks", description = "A plugin that allows you to set a different resource pack for each server.", authors = {"onebeastchris"})
public class GeyserPerServerPacksVelocity implements EventRegistrar {
    private final LoggerImpl logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private GeyserPerServerPack plugin;

    private HashMap<String, RegisteredServer> playerCache;

    // needed as a temporary workaround until I can figure out how to always apply the correct pack.
    private ArrayList<String> xuidsWithDefaultPack;

    @Inject
    public GeyserPerServerPacksVelocity(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        this.logger = new LoggerImpl(logger);
        this.dataDirectory = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Configurate config = Configurate.create(this.dataDirectory);
        boolean hasGeyser = server.getPluginManager().isLoaded("Geyser-Velocity");

        if (!hasGeyser) {
            logger.error("There is no Geyser plugin detected!");
            return;
        }

        plugin = new GeyserPerServerPack(this.dataDirectory, config, logger);

        playerCache = new HashMap<>();
        xuidsWithDefaultPack = new ArrayList<>();

        GeyserApi.api().eventBus().register(this, this);

        logger.info("GeyserPerServerPacks has been enabled!");

        if (config.isDebug()) {
            logger.info("Debug mode is enabled!");
            for (RegisteredServer server : this.server.getAllServers()) {
                logger.info("Server: " + server.getServerInfo().getName());
                logger.info("Packs: " + plugin.getPacks(server.getServerInfo().getName()));
            }
        }
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPlayerChangeServer(ServerPreConnectEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();
        //Check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(uuid)) {
            logger.debug("Player " + uuid + " is not a bedrock player!");
            return;
        }

        if (event.getResult().getServer().isEmpty()) {
            logger.debug("Server is empty");
            return;
        }

        if (!event.getResult().isAllowed()) {
            logger.debug("Server is not allowed");
            return;
        }

        GeyserConnection connection = GeyserApi.api().connectionByUuid(uuid);
        if (connection == null) {
            logger.error("Player " + uuid + " is not connected to a server!");
            return;
        }
        String xuid = connection.xuid();
        if (playerCache.containsKey(xuid)) {
            logger.debug("contains xuid");
            if (playerCache.get(xuid) == null) {
                logger.error("Player " + xuid + " is not connected to a server!");
                return;
            }
            //If the player is already connected to a server, we need to remove them from the cache
            logger.debug("Player " + xuid + " is known to us, redirecting to " + playerCache.get(xuid).getServerInfo().getName());
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(playerCache.get(xuid)));
            playerCache.remove(xuid);
            return;
        } else {
            logger.debug("does not contain xuid");
            //If the player is not in the cache, we need to add them to the cache
            try {
                playerCache.put(xuid, event.getResult().getServer().get());
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                connection.transfer(plugin.getConfig().getAddress(), plugin.getConfig().getPort());
            } catch (Exception e) {
                logger.error("Player " + xuid + " is not connecting to a server!");
                return;
            }
        }
    }


    @org.geysermc.event.subscribe.Subscribe
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();

        if (playerCache.containsKey(xuid)) {
            String server = playerCache.get(xuid).getServerInfo().getName();
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
