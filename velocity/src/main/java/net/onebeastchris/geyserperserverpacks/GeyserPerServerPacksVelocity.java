package net.onebeastchris.geyserperserverpacks;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.utils.FloodgateJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.utils.GeyserJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.utils.JavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.Permission;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "geyserperserverpacks",
        name = "GeyserPerServerPacks",
        version = "1.0-SNAPSHOT"
)
public class GeyserPerServerPacksVelocity {
    private final Logger logger;
    private final ProxyServer server;
    private Configurate config = null;
    private final Path dataDirectory;
    private JavaPlayerChecker playerChecker;
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
        boolean hasFloodgate = server.getPluginManager().isLoaded("floodgate");
        boolean hasGeyser = server.getPluginManager().isLoaded("Geyser-Velocity");

        if (!hasFloodgate && !hasGeyser) {
            logger.warn("There is no Geyser or Floodgate plugin detected!");
            return;
        }

        if (hasFloodgate) {
            this.playerChecker = new FloodgateJavaPlayerChecker();
        } else {
            this.playerChecker = new GeyserJavaPlayerChecker();
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChangeServer(ServerConnectedEvent event) {

        //Check if the player is a bedrock player
        if (!this.playerChecker.isBedrockPlayer(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getPlayer().hasPermission(Permission.bypassPermission)) {
            return;
        }

        String servername = event.getServer().getServerInfo().getName();

    }

    public static TextComponent color(String s) {
        return serializer.deserialize(s);
    }
}
