package net.onebeastchris.geyserpacksync;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.event.EventPriority;
import net.onebeastchris.geyserpacksync.common.Configurate;
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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class GeyserPackSyncBungee extends Plugin implements Listener, EventRegistrar {
    private PSPLogger logger;
    private GeyserPackSync plugin;
    private HashMap<String, ServerInfo> playerCache;
    private HashMap<UUID, String> tempUntilServerKnown;

    @Override
    public void onEnable() {
        Configurate config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;
        logger = new LoggerImpl(this.getLogger());

        if (!hasGeyser) {
            getLogger().severe("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        if (configChecks(config)) {
            return;
        }

        plugin = new GeyserPackSync(this.getDataFolder().toPath(), config, logger);
        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        GeyserApi.api().eventBus().register(this, this);

        if (config.isDebug()) {
            for (ServerInfo server : getProxy().getServers().values()) {
                logger.debug("Server: " + server.getName());
                logger.debug("Packs: " + plugin.getPacks(server.getName()));
            }
        }
        getLogger().info("GeyserPackSync has been enabled!");
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

    @EventHandler(priority = EventPriority.HIGH)
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

    public boolean reload() {
        playerCache.clear();
        tempUntilServerKnown.clear();

        Configurate config = Configurate.create(this.getDataFolder().toPath());

        if (config == null) {
            logger.error("There was an error loading the config!");
            return false;
        }

        plugin = new GeyserPackSync(this.getDataFolder().toPath(), config, logger);
        playerCache = new HashMap<>();
        tempUntilServerKnown = new HashMap<>();

        logger.info("GeyserPerServerPacks has been reloaded!");
        logger.setDebug(config.isDebug());

        logger.debug("Debug mode is enabled!");
        return configChecks(config);
    }

    public boolean configChecks(Configurate config) {
        if (config == null) {
            logger.error("There was an error loading the config!");
            return false;
        }

        if (config.getPort() <= 0 || config.getPort() > 65535) {
            logger.error("Invalid port! Please set a valid port in the config!");
            return false;
        }

        if (config.getAddress() == null || config.getAddress().isEmpty()) {
            logger.error("Invalid address! Please set a valid address in the config!");
            return false;
        }

        logger.setDebug(config.isDebug());
        logger.debug("Debug mode is enabled");
        return true;
    }

    public class ReloadCommand extends Command {
        public ReloadCommand() {
            super("reloadpacks");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(new TextComponent("§aReloading GeyserPerServerPacks..."));
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
