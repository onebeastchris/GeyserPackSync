package net.onebeastchris.geyserperserverpacks;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.onebeastchris.geyserperserverpacks.common.GeyserPerServerPackBootstrap;
import net.onebeastchris.geyserperserverpacks.common.PSPLogger;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Plugin(
        id = "geyserperserverpacks",
        name = "GeyserPerServerPacks",
        version = "1.0-SNAPSHOT"
)
public class GeyserPerServerPacksVelocity implements GeyserPerServerPackBootstrap {
    private final Logger logger;
    private final ProxyServer server;
    private Configurate config = null;
    private final Path dataDirectory;
    private static LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().character('&').hexCharacter('#').hexColors().build();

    @Inject
    public GeyserPerServerPacksVelocity(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = Configurate.create(this.dataDirectory);
        boolean hasGeyser = server.getPluginManager().isLoaded("Geyser-Velocity");

        if (!hasGeyser) {
            logger.warn("There is no Geyser or Floodgate plugin detected!");
            return;
        }


    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChangeServer(ServerPreConnectEvent event) {

        //Check if the player is a bedrock player
        if (!GeyserApi.api().isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        // TODO

    }

    public static TextComponent color(String s) {
        return serializer.deserialize(s);
    }

    @Override
    public Path dataFolder() {
        return null;
    }

    @Override
    public Configurate config() {
        return null;
    }

    @Override
    public PSPLogger logger() {
        return null ;
    }

    @Override
    public Map<String, List<ResourcePack>> packs() {
        return null;
    }
}
