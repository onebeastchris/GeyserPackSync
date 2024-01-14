package net.onebeastchris.geyserpacksync;

import io.netty.channel.Channel;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.event.EventPriority;
import net.onebeastchris.geyserpacksync.common.GeyserPackSyncBootstrap;
import net.onebeastchris.geyserpacksync.common.utils.BackendServer;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncConfig;
import net.onebeastchris.geyserpacksync.common.GeyserPackSync;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserpacksync.common.utils.PackSyncLogger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.*;

public final class GeyserPackSyncBungee extends Plugin implements Listener, EventRegistrar, GeyserPackSyncBootstrap {
    private PackSyncLogger logger;
    public GeyserPackSync plugin;
    private PackSyncConfig config;
    private boolean floodgatePresent;

    @Override
    public void onEnable() {
        floodgatePresent = getProxy().getPluginManager().getPlugin("floodgate") != null;
        config = PackSyncConfig.create(this.getDataFolder().toPath());
        logger = new LoggerImpl(this.getLogger());

        if (!PackSyncConfig.checkConfig(logger, config)) {
            logger.error("Disabling due to invalid config!");
            return;
        }

        plugin = new GeyserPackSync(this);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());

        GeyserApi.api().eventBus().register(this, this);
        GeyserApi.api().eventBus().subscribe(this, SessionLoadResourcePacksEvent.class, this::onGeyserResourcePackRequest);
        GeyserApi.api().eventBus().subscribe(this, SessionDisconnectEvent.class, this::onGeyserDisconnectEvent);

        logger.setDebug(config.isDebug());
        if (config.isDebug()) {
            logger.debug("Debug mode enabled!");
            for (ServerInfo server : getProxy().getServers().values()) {
                logger.debug("Server: " + server.getName());
                logger.debug("Packs: " + plugin.getPacks(backendFromName(server.getName())));
            }
        }

        getLogger().info("GeyserPackSync has been enabled!");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFirstConnect(ServerConnectEvent event) {
        final UserConnection connection = (UserConnection) event.getPlayer();
        final Channel channel = connection.getCh().getHandle();

        Optional<BackendServer> backendServer = plugin.handleFirst(
                BungeeBackendServer.of(event.getTarget()),
                channel,
                event.getPlayer().getUniqueId());
        backendServer.ifPresent(server -> event.setTarget(((BungeeBackendServer) server).info()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnectEvent(ServerConnectEvent event) {
        UserConnection connection = (UserConnection) event.getPlayer();
        Channel channel = connection.getCh().getHandle();

        Optional<BackendServer> backendServer = plugin.handleLast(
                BungeeBackendServer.of(event.getTarget()),
                channel,
                event.getPlayer().getUniqueId(),
                event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY || event.getReason() == ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT,
                (String message) -> event.getPlayer().disconnect(TextComponent.fromLegacy(message))
        );

        backendServer.ifPresent(server -> event.setTarget(((BungeeBackendServer) server).info()));
    }

    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        plugin.handleGeyserLoadResourcePackEvent(event);
    }

    public void onGeyserDisconnectEvent(SessionDisconnectEvent event) {
        plugin.handleDisconnectEvent(event);
    }

    public boolean reload() {
        PackSyncConfig config = PackSyncConfig.create(this.getDataFolder().toPath());

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
        ServerInfo info = getProxy().getServers().get(name);
        if (info == null) return null;
        return BungeeBackendServer.of(info);
    }

    @Override
    public Path dataFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public PackSyncConfig config() {
        return config;
    }

    @Override
    public boolean floodgatePresent() {
        return floodgatePresent;
    }

    public class ReloadCommand extends Command {
        public ReloadCommand() {
            super("reloadpacks", "geyserpacksync.reload", "packsyncreload");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(TextComponent.fromLegacy("§eReloading GeyserPerServerPacks..."));
            if (GeyserPackSyncBungee.this.reload()) {
                if (plugin.getConfig().isDebug()) {
                    for (ServerInfo server : getProxy().getServers().values()) {
                        logger.debug("Server: " + server.getName());
                        logger.debug("Packs: " + plugin.getPacks(backendFromName(server.getName())));
                    }
                }
                sender.sendMessage(TextComponent.fromLegacy("§aGeyserPerServerPacks has been reloaded!"));
            } else {
                sender.sendMessage(TextComponent.fromLegacy("§cThere was an error reloading GeyserPerServerPacks!"));
            }
        }
    }
}
