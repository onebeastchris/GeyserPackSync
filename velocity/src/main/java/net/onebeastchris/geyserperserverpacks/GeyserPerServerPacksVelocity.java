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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(
        id = "geyserperserverpacks",
        name = "GeyserPerServerPacks",
        version = "1.0-SNAPSHOT"
)
public class GeyserPerServerPacksVelocity {
    private final LoggerImpl logger;
    private final ProxyServer server;
    private Configurate config = null;
    private final Path dataDirectory;
    private GeyserPerServerPack plugin;

    private HashMap<String, ServerPreConnectEvent.ServerResult> playerCache = new HashMap<>();


    @Inject
    public GeyserPerServerPacksVelocity(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        this.logger = new LoggerImpl(logger);
        this.dataDirectory = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = Configurate.create(this.dataDirectory);
        boolean hasGeyser = server.getPluginManager().isLoaded("Geyser-Velocity");

        if (!hasGeyser) {
            logger.error("There is no Geyser plugin detected!");
            return;
        }

        plugin = new GeyserPerServerPack(this.dataDirectory, config, logger);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChangeServer(ServerPreConnectEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();
        //Check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(uuid)) {
            return;
        }

        GeyserConnection connection = GeyserApi.api().connectionByUuid(uuid);
        if (connection == null) {
            return;
        }
        String xuid = connection.xuid();

        ServerPreConnectEvent.ServerResult result = event.getResult();

        if (playerCache.get(xuid) != null) {
            //If the player is already connected to a server, we need to remove them from the cache
            event.setResult(playerCache.get(xuid));
            playerCache.remove(xuid);
        } else {
            //If the player is not in the cache, we need to add them to the cache
            playerCache.put(xuid, result);
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            connection.transfer(plugin.getConfig().getAddress(), plugin.getConfig().getPort());
        }
    }


    @org.geysermc.event.subscribe.Subscribe
    public void onGeyserLogin(SessionInitializeEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserConnection connection = event.connection();
        // TODO: can we use forced hosts to get us the server name, so we can directly send the right packs?
    }

    @org.geysermc.event.subscribe.Subscribe
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();
        Optional<RegisteredServer> server = playerCache.get(xuid).getServer();

        if (server.isEmpty()) {
            return;
        }

        List<ResourcePack> packs = plugin.getPacks(server.get().getServerInfo().getName());
        for (ResourcePack pack : packs) {
            event.register(pack);
        }
    }
}
