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
//import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onebeastchris.geyserpacksync.common.GeyserPackSyncBootstrap;
import net.onebeastchris.geyserpacksync.common.utils.BackendServer;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncLogger;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncConfig;
import net.onebeastchris.geyserpacksync.common.GeyserPackSync;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Plugin(
        id = "geyserpacksync",
        name = "GeyserPackSync",
        version = "2.0-SNAPSHOT",
        description = "GeyserPackSync is a plugin that allows you to set a different Bedrock edition resource pack(s) per server.",
        authors = {"onebeastchris"},
        dependencies = {
            @Dependency(id = "geyser"),
            @Dependency(id = "floodgate", optional = true)
        }
)
public class GeyserPackSyncVelocity implements EventRegistrar, GeyserPackSyncBootstrap {
    private final LoggerImpl logger;
    private final ProxyServer server;
    private final Path dataFolder;
    private GeyserPackSync plugin;
    private PackSyncConfig config;
    private boolean floodgatePresent;

    @Inject
    public GeyserPackSyncVelocity(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        this.logger = new LoggerImpl(logger);
        this.dataFolder = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = PackSyncConfig.create(this.dataFolder);
        floodgatePresent= server.getPluginManager().isLoaded("floodgate");

        if (!PackSyncConfig.checkConfig(logger, config)) {
            return;
        }

        try {
            VelocityAccessor.init();
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
        }

        plugin = new GeyserPackSync(this);

        GeyserApi.api().eventBus().register(this, this);
        GeyserApi.api().eventBus().subscribe(this, SessionLoadResourcePacksEvent.class, this::onGeyserResourcePackRequest);
        GeyserApi.api().eventBus().subscribe(this, SessionDisconnectEvent.class, this::onGeyserDisconnectEvent);

        logger.info("GeyserPackSync has been enabled!");

        logger.setDebug(config.isDebug());
        logger.debug("Debug mode is enabled!");
        for (RegisteredServer server : this.server.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            logger.debug("Server: " + serverName);
            logger.debug("Packs: " + plugin.getPacks(Objects.requireNonNull(backendFromName(serverName))));
        }

        CommandManager commandManager = server.getCommandManager();
        CommandMeta meta = commandManager
                .metaBuilder("packsyncreload")
                .aliases("reloadpacks")
                .build();
        SimpleCommand simpleCommand = new ReloadCommand();
        commandManager.register(meta, simpleCommand);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onConnect(ServerPreConnectEvent event) {
        Optional<RegisteredServer> serverresult = event.getResult().getServer();
        if (serverresult.isEmpty()) {
            logger.warning("Got an error: serverresult empty in a ServerPreConnectEvent?!");
            return;
        }

        //final ConnectedPlayer connectedPlayer = (ConnectedPlayer) event.getPlayer();
        //final Channel channel = connectedPlayer.getConnection().getChannel();

        Channel channel = VelocityAccessor.getChannel(event.getPlayer());

        Optional<BackendServer> backendServer = plugin.handleFirst(
                VelocityBackendServer.of(serverresult.get()),
                channel,
                event.getPlayer().getUniqueId());

        if (backendServer.isPresent()) {
            RegisteredServer registeredServer = ((VelocityBackendServer) backendServer.get()).registeredServer();
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(registeredServer));
        }
    }

    // late: grab server if needed. Last to not break compat with other plugins changing the destination server.
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChangeServer(ServerPreConnectEvent event) {
        var serverresult = event.getResult().getServer();
        if (serverresult.isEmpty()) {
            logger.warning("Got an error serverresult from a ServerPreConnectEvent?!");
            return;
        }

        //final ConnectedPlayer connectedPlayer = (ConnectedPlayer) event.getPlayer();
        //final Channel channel = connectedPlayer.getConnection().getChannel();

        Channel channel = VelocityAccessor.getChannel(event.getPlayer());

        Optional<BackendServer> backendServer = plugin.handleLast(
                VelocityBackendServer.of(serverresult.get()),
                channel,
                event.getPlayer().getUniqueId(),
                event.getOriginalServer() == null,
                (message -> event.getPlayer().disconnect(Component.text(message)))
        );

        if (backendServer.isPresent()) {
            Optional<RegisteredServer> registeredServer = server.getServer(backendServer.get().name());
            if (registeredServer.isEmpty()) {
                logger.error("Could not find server with name " + backendServer.get().name());
                return;
            }
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(registeredServer.get()));
        }
    }

    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        plugin.handleGeyserLoadResourcePackEvent(event);
    }

    public void onGeyserDisconnectEvent(SessionDisconnectEvent event) {
        plugin.handleDisconnectEvent(event);
    }

    public boolean reload() {
        PackSyncConfig config = PackSyncConfig.create(this.dataFolder);

        if (config == null || !PackSyncConfig.checkConfig(logger, config)) {
            logger.error("There was an error loading the config!");
            return false;
        }

        logger.setDebug(config.isDebug());
        plugin.reload();
        return true;
    }

    @Override
    public PackSyncLogger logger() {
        return this.logger;
    }

    @Override
    public @Nullable BackendServer backendFromName(String name) {
        Optional<RegisteredServer> optional = this.server.getServer(name);
        return optional.map(VelocityBackendServer::of).orElse(null);
    }

    @Override
    public Path dataFolder() {
        return dataFolder;
    }

    @Override
    public PackSyncConfig config() {
        return config;
    }

    @Override
    public boolean floodgatePresent() {
        return floodgatePresent;
    }

    public final class ReloadCommand implements SimpleCommand {

        @Override
        public void execute(final Invocation invocation) {
            CommandSource source = invocation.source();
            if (reload()) {
                for (RegisteredServer server : server.getAllServers()) {
                    logger.debug("Server: " + server.getServerInfo().getName());
                    logger.debug("Packs: " + plugin.getPacks(VelocityBackendServer.of(server)));
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
