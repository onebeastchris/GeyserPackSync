package net.onebeastchris.geyserpacksync;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onebeastchris.geyserpacksync.common.utils.Configurate;
import net.onebeastchris.geyserpacksync.common.GeyserPackSync;
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
import java.util.concurrent.CompletableFuture;

@Plugin(
        id = "geyserpacksync",
        name = "GeyserPackSync",
        version = "1.1-SNAPSHOT",
        description = "GeyserPackSync is a plugin that allows you to set a different Bedrock edition resource pack(s) per server.",
        authors = {"onebeastchris"},
        dependencies = {
            @Dependency(id = "geyser")
        }
)
public class GeyserPackSyncVelocity implements EventRegistrar {
    private final LoggerImpl logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private GeyserPackSync plugin;
    private HashMap<String, RegisteredServer> playerCache;
    private HashMap<UUID, String> tempUntilServerKnown;
    private final List<String> unknownPacks = new ArrayList<>();

    @Inject
    public GeyserPackSyncVelocity(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        this.logger = new LoggerImpl(logger);
        this.dataDirectory = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Configurate config = Configurate.create(this.dataDirectory);
        boolean hasGeyser = server.getPluginManager().isLoaded("geyser");

        if (!hasGeyser) {
            logger.error("There is no Geyser plugin detected!");
            return;
        }

        if (!Configurate.checkConfig(logger, config)) {
            return;
        }

        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();
        plugin = new GeyserPackSync(this.dataDirectory, config, logger);

        GeyserApi.api().eventBus().register(this, this);
        GeyserApi.api().eventBus().subscribe(this, SessionLoadResourcePacksEvent.class, this::onGeyserResourcePackRequest);

        logger.info("GeyserPerServerPacks has been enabled!");

        logger.setDebug(config.isDebug());
        logger.debug("Debug mode is enabled!");
        for (RegisteredServer server : this.server.getAllServers()) {
            logger.debug("Server: " + server.getServerInfo().getName());
            logger.debug("Packs: " + plugin.getPacks(server.getServerInfo().getName()));
        }

        CommandManager commandManager = server.getCommandManager();
        CommandMeta meta = commandManager
                .metaBuilder("packsyncreload")
                .aliases("reloadpacks")
                .build();
        SimpleCommand simpleCommand = new ReloadCommand();
        commandManager.register(meta, simpleCommand);
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onConnect(ServerPreConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        GeyserConnection connection = GeyserApi.api().connectionByUuid(uuid);
        if (connection == null) {
            logger.debug("GeyserConnection is null for player " + uuid + "!");

            // temporary workaround for https://github.com/GeyserMC/Geyser/issues/3456
            for (GeyserConnection c : GeyserApi.api().onlineConnections()) {
                if (c.javaUuid().equals(uuid)) {
                    connection = c;
                    break;
                }
            }

            if (connection == null) {
                logger.debug("GeyserConnection is still null for player " + uuid + "!");
                return;
            }
        }

        String xuid = connection.xuid();
        if (playerCache.containsKey(xuid)) {
            RegisteredServer server = playerCache.remove(xuid);
            logger.debug("Player " + xuid + " is known to us, redirecting to " + server.getServerInfo().getName());
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(server));
        } else {
            logger.debug("does not contain xuid");
            // grab server once event is completed to not break compat with other plugins changing the destination server.
            tempUntilServerKnown.put(uuid, xuid);
        }
    }

    // late: grab server if needed. Last to not break compat with other plugins changing the destination server.
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChangeServer(ServerPreConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        //Check if the player is a bedrock player
        if (!tempUntilServerKnown.containsKey(uuid)) {
            logger.debug("No need to grab server - probably a Java player.");
            return;
        }

        if (event.getResult().getServer().isEmpty() || !event.getResult().isAllowed()) {
            logger.debug("Server is empty/not allowed. Not caching/transferring.");
            tempUntilServerKnown.remove(uuid);
            return;
        }

        String xuid = tempUntilServerKnown.remove(uuid);
        RegisteredServer server = event.getResult().getServer().get();
        playerCache.put(xuid, server);

        boolean isFirstConnection = unknownPacks.remove(xuid);
        logger.debug("isFirstConnection: " + isFirstConnection);
        // ugly, yes, but until we use void/limbo servers, this is the only way :(
        if (isFirstConnection) {
            if (server.getServerInfo().getName().equals(plugin.getConfig().getDefaultServer())) {
                logger.debug("Player " + xuid + " has the default pack, and is going to the default server, allowing connection");
                playerCache.remove(xuid);
            } else {
                if (plugin.getConfig().isKickOnMismatch()) {
                    logger.debug("Player " + xuid + " has the default pack, but is connecting to " + server.getServerInfo().getName() + ", kicking");
                    event.getPlayer().disconnect(Component.text(plugin.getConfig().getKickMessage()));
                } else {
                    logger.warning("Player " + xuid + " has the default pack, but is connecting to " + server.getServerInfo().getName() + ".");
                }
            }
        } else {
            logger.debug("Player " + xuid + " is being reconnected...");
            GeyserApi.api().transfer(uuid, plugin.getConfig().getAddress(), plugin.getConfig().getPort());
        }
    }


    //@org.geysermc.event.subscribe.Subscribe - https://github.com/GeyserMC/Geyser/issues/3694 goes brrrrrrrr
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();
        List<ResourcePack> packs = new ArrayList<>();

        if (playerCache.containsKey(xuid)) {
            String server = playerCache.get(xuid).getServerInfo().getName();
            logger.debug("Player " + xuid + " is known to us, so we send server packs for " + server);
            if (server != null) packs = plugin.getPacks(server);
        } else {
            logger.debug("Player " + xuid + " is not known to us, so we send default packs");
            packs = plugin.getPacks(plugin.getConfig().getDefaultServer());
            unknownPacks.add(xuid);
        }
        packs.forEach(event::register);
    }

    public boolean reload() {
        playerCache.clear();
        tempUntilServerKnown.clear();
        unknownPacks.clear();

        Configurate config = Configurate.create(this.dataDirectory);

        if (config == null) {
            logger.error("There was an error loading the config!");
            return false;
        }

        plugin = new GeyserPackSync(this.dataDirectory, config, logger);
        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();

        logger.info("GeyserPerServerPacks was reloaded!");
        logger.setDebug(config.isDebug());
        logger.debug("Debug mode is enabled!");

        return Configurate.checkConfig(logger, config);
    }

    public final class ReloadCommand implements SimpleCommand {

        @Override
        public void execute(final Invocation invocation) {
            CommandSource source = invocation.source();
            if (reload()) {
                for (RegisteredServer server : server.getAllServers()) {
                    logger.debug("Server: " + server.getServerInfo().getName());
                    logger.debug("Packs: " + plugin.getPacks(server.getServerInfo().getName()));
                }
                source.sendMessage(Component.text("Reload successful!").color(NamedTextColor.DARK_GREEN));
            } else {
                source.sendMessage(Component.text("Reload failed! Check the console for more information.").color(NamedTextColor.RED));
            }
        }

        @Override
        public boolean hasPermission(final Invocation invocation) {
            return invocation.source().hasPermission("geyserpacksync.reload");
        }

        @Override
        public List<String> suggest(final Invocation invocation) {
            return List.of();
        }

        @Override
        public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
            return CompletableFuture.completedFuture(List.of());
        }
    }
}
