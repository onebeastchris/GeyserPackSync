package net.onebeastchris.geyserperserverpacks;

import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.ResourcePackLoader;
import net.onebeastchris.geyserperserverpacks.common.utils.FloodgateJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.utils.GeyserJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPackBootstrap;
import net.onebeastchris.geyserperserverpacks.common.utils.JavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.Permission;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.session.GeyserSession;

import java.nio.file.Path;

public final class GeyserPerServerPacksBungee extends Plugin implements Listener, GeyserPerServerPackBootstrap, EventRegistrar {
    private JavaPlayerChecker playerChecker;
    public Configurate config;
    public PSPLogger logger;
    public ResourcePackLoader resourcePackLoader;

    public PlayerStorageImpl playerStorage;


    @Override
    public void onEnable() {
        config = Configurate.create(this.getDataFolder().toPath());
        boolean hasFloodgate = getProxy().getPluginManager().getPlugin("floodgate") != null;
        boolean hasGeyser = getProxy().getPluginManager().getPlugin("Geyser-BungeeCord") != null;

        logger = new LoggerImpl(getLogger());
        resourcePackLoader = new ResourcePackLoader(logger);
        playerStorage = new PlayerStorageImpl();

        if (!hasFloodgate && !hasGeyser) {
            getLogger().warning("There is no Geyser or Floodgate plugin detected! Disabling...");
            onDisable();
            return;
        }

        if (hasFloodgate) {
            this.playerChecker = new FloodgateJavaPlayerChecker();
        } else {
            this.playerChecker = new GeyserJavaPlayerChecker();
        }

        getProxy().getPluginManager().registerListener(this, this);
        GeyserApi.api().eventBus().register(this, this);
    }

    @Override
    public Configurate getConfig() {
        return config;
    }

    @Override
    public ResourcePackLoader getResourcePackLoader() {
        return resourcePackLoader;
    }

    @Override
    public PSPLogger getGPSPLogger() {
        return logger;
    }

    @Override
    public Path packsDataFolders() {
        return this.getDataFolder().toPath().resolve("packs");
    }

    @Override
    public PlayerStorageImpl getPlayerStorage() {
        return playerStorage;
    }


    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        if (event.getPlayer().hasPermission(Permission.bypassPermission)) {
            return;
        }

        boolean isBedrockPlayer = this.playerChecker.isBedrockPlayer(event.getPlayer().getUniqueId());
        String servername = event.getServer().getInfo().getName();
    }

    @Subscribe
    public void onGeyserSessionInitialize(SessionInitializeEvent event) {
        //if we can get the server name from the event, we can use that to get the right resource pack before the player joins the server
        GeyserSession session = (GeyserSession) event.connection();
        if (session.getGeyser().getConfig().getRemote().isForwardHost()) {
            String servername = session.get;
            //do stuff here. Like; send the player the right resource pack
        }
    }

    @Subscribe
    public void onGeyserResourcePackRequest(SessionLoadResourcePacksEvent event) {
        //do stuff here. Like; send the player the right resource pack

    }
}
