package net.onebeastchris.geyserpacksync;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.event.EventPriority;
import net.onebeastchris.geyserpacksync.common.utils.Configurate;
import net.onebeastchris.geyserpacksync.common.GeyserPackSync;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserpacksync.common.PSPLogger;
import org.geysermc.api.connection.Connection;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class GeyserPackSyncBungee extends Plugin implements Listener, EventRegistrar {
    private PSPLogger logger;
    public GeyserPackSync plugin;
    private HashMap<String, ServerInfo> playerCache = new HashMap<>();
    private HashMap<UUID, String> tempUntilServerKnown = new HashMap<>();

    @Override
    public void onEnable() {
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;
        if (!hasGeyser) {
            getLogger().severe("There is no Geyser or Floodgate plugin detected! Disabling...");
            return;
        }

        Configurate config = Configurate.create(this.getDataFolder().toPath());
        logger = new LoggerImpl(this.getLogger());

        if (!Configurate.checkConfig(logger, config)) {
            logger.error("Disabling due to invalid config!");
            return;
        }

        plugin = new GeyserPackSync(this.getDataFolder().toPath(), config, logger);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        GeyserApi.api().eventBus().register(this, this);

        logger.setDebug(config.isDebug());
        if (config.isDebug()) {
            logger.debug("Debug mode enabled!");
            for (ServerInfo server : getProxy().getServers().values()) {
                logger.debug("Server: " + server.getName());
                logger.debug("Packs: " + plugin.getPacks(server.getName()));
            }
        }
        getLogger().info("GeyserPackSync has been enabled!");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onConnect(ServerConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Connection connection = GeyserApi.api().connectionByUuid(uuid);
        if (connection == null) {
            logger.debug("GeyserConnection is null for player " + uuid);
            return;
        }

        String xuid = connection.xuid();
        if (playerCache.containsKey(xuid)) {
            logger.debug("Player " + xuid + " is known to us, redirecting to " + playerCache.get(xuid).getName());
            event.setTarget(playerCache.remove(xuid));
            playerCache.remove(xuid);
        } else {
            logger.debug("does not contain xuid");
            // grab server once event is completed to not break compat with other plugins changing the destination server.
            tempUntilServerKnown.put(uuid, xuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnectEvent(ServerConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        //Check if the player is a bedrock player
        if (!tempUntilServerKnown.containsKey(uuid)) {
            logger.debug("No need to grab server!");
            return;
        }

        String xuid = tempUntilServerKnown.remove(uuid);
        ServerInfo server = event.getTarget();

        if (!event.getTarget().canAccess(event.getPlayer())) {
            logger.debug("Player " + event.getPlayer().getName() + " can't access " + server.getName());
            return;
        }

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

    @Subscribe
    @SuppressWarnings("unused")
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();
        List<ResourcePack> packs = new ArrayList<>();

        if (playerCache.containsKey(xuid)) {
            String server = playerCache.get(xuid).getName();
            logger.debug("Player " + xuid + " is known to us, so we send server packs for " + server);
            if (server != null) {
                packs = plugin.getPacks(server);
            }
        } else {
            logger.debug("Player " + xuid + " is not known to us, so we send default packs");
            packs = plugin.getPacks(plugin.getConfig().getDefaultServer());
        }
        packs.forEach(event::register);
    }

    public boolean reload() {
        playerCache.clear();
        tempUntilServerKnown.clear();
        Configurate config = Configurate.create(this.getDataFolder().toPath());

        plugin = new GeyserPackSync(this.getDataFolder().toPath(), config, logger);
        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();

        logger.info("GeyserPerServerPacks has been reloaded!");
        logger.setDebug(config.isDebug());
        return Configurate.checkConfig(logger, config);
    }

    public class ReloadCommand extends Command {
        public ReloadCommand() {
            super("reloadpacks", "geyserpacksync.reload", "packsyncreload");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(new TextComponent("§eReloading GeyserPerServerPacks..."));
            if (reload()) {
                if (plugin.getConfig().isDebug()) {
                    for (ServerInfo server : getProxy().getServers().values()) {
                        logger.debug("Server: " + server.getName());
                        logger.debug("Packs: " + plugin.getPacks(server.getName()));
                    }
                }
                sender.sendMessage(new TextComponent("§aGeyserPerServerPacks has been reloaded!"));
            } else {
                sender.sendMessage(new TextComponent("§cThere was an error reloading GeyserPerServerPacks!"));
            }
        }
    }
}
