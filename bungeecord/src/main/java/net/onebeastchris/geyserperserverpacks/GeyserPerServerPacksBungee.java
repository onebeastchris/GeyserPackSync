package net.onebeastchris.geyserperserverpacks;

import net.md_5.bungee.api.event.ServerConnectEvent;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPack;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.util.List;
import java.util.UUID;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, EventRegistrar {
    private Configurate config;
    private PSPLogger logger;
    private GeyserPerServerPack plugin;
    private int port;
    private String address;

    @Override
    public void onEnable() {
        config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        logger = new LoggerImpl(this.getLogger());

        if (!hasGeyser) {
            getLogger().warning("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        plugin = new GeyserPerServerPack(this.getDataFolder().toPath(), config, logger);

        port = GeyserApi.api().bedrockListener().port();
        String configAddress = GeyserApi.api().bedrockListener().address();
        address = configAddress.equals("0.0.0.0") ? GeyserApi.api().defaultRemoteServer().address() : configAddress;

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        // TODO: if this is the first time the *bedrock* player is joining the server, cancel, save, and reconnect.
        // If this is the second time, set the server from the initial attempt and continue "where we left off"
        // we should store the "current" server for a bedrock player.
        if (GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();
        if (plugin.getServerFromCache(playerUUID) != null) {
            // The player is known to us, so we can just send them to the server they tried to connect to before
            event.setTarget(getProxy().getServerInfo(plugin.getServerFromCache(playerUUID)));
            plugin.removePlayerFromCache(playerUUID);
        } else {
            // The player is not known to us, so we need to save the server they tried to connect to and reconnect them to apply packs
            plugin.addPlayerToCache(playerUUID, event.getTarget().getName());
            event.setCancelled(true);
            GeyserApi.api().transfer(playerUUID, address, port);
        }

    }


    @Subscribe
    public void onGeyserLogin(SessionLoginEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserConnection connection = event.connection();
        // TODO: can we use forced hosts to get us the server name, so we can directly send the right packs?
    }

    @Subscribe
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        UUID uuid = event.connection().javaUuid();
        String server = plugin.getServerFromCache(uuid);
        if (server != null) {
            List<ResourcePack> packs = plugin.getPacks(server);
            for (ResourcePack pack : packs) {
                event.register(pack);
            }
        }
    }
}
