package net.onebeastchris.geyserperserverpacks;

import net.md_5.bungee.api.event.ServerConnectEvent;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.ResourcePackLoader;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPackBootstrap;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import net.onebeastchris.geyserperserverpacks.common.utils.PlayerStorage;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, GeyserPerServerPackBootstrap, EventRegistrar {
    private Configurate config;
    private PSPLogger logger;
    private PlayerStorage playerStorage;
    private Map<String, List<ResourcePack>> packs;

    @Override
    public void onEnable() {
        config = Configurate.create(this.getDataFolder().toPath());
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        logger = new LoggerImpl(getLogger());
        packs = ResourcePackLoader.loadPacks(this);

        if (!hasGeyser) {
            getLogger().warning("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }
        this.playerStorage = new PlayerStorage();

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);
    }

    @Override
    public Path dataFolder() {
        return this.getDataFolder().toPath();
    }

    @Override
    public Configurate config() {
        return this.config;
    }

    @Override
    public PSPLogger logger() {
        return this.logger;
    }

    @Override
    public Map<String, List<ResourcePack>> packs() {
        return this.packs;
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        // TODO: if this is the first time the *bedrock* player is joining the server, cancel, save, and reconnect.
        // If this is the second time, set the server from the initial attempt and continue "where we left off"
        // we should store the "current" server for a bedrock player.
        if (GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

    }


    @Subscribe
    public void onGeyserLogin(SessionLoginEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserConnection connection = event.connection();
        // TODO: do we need this? e.g. to check if the session reconnected successfully
    }

    @Subscribe
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        //do stuff here. Like; send the player the right resource pack
        GeyserConnection connection = event.connection();
        // TODO: get & send target server's resource packs
    }
}
