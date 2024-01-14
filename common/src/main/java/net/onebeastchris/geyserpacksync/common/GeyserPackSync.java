package net.onebeastchris.geyserpacksync.common;
import io.netty.channel.Channel;
import net.onebeastchris.geyserpacksync.common.utils.*;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class GeyserPackSync {
    private static GeyserPackSyncBootstrap BOOTSTRAP;

    // Stores the resource pack list for each backend server
    private Map<String, List<ResourcePack>> packs;
    private final BedrockChecker checker;

    // Stores the currently known backend server for each xuid - to keep track of packs
    private final Map<String, BackendServer> playerPackTracker = new HashMap<>();

    // Keeps track of all the currently ongoing server switch changes (or requests thereof)
    private final Map<String, BackendServer> inProgress = new HashMap<>();
    private BackendServer defaultServer;

    public GeyserPackSync(GeyserPackSyncBootstrap bootstrap) {
        GeyserPackSync.BOOTSTRAP = bootstrap;

        this.packs = ResourcePackLoader.loadPacks(this);
        this.checker = new BedrockChecker(bootstrap.floodgatePresent());

        this.defaultServer = bootstrap.backendFromName(bootstrap.config().getDefaultServer());
        if (defaultServer == null) {
            getLogger().error("Default server is null! Please set a default server in the config.");
        }

        if (!bootstrap.floodgatePresent()) {
            GeyserPackSync.BOOTSTRAP.logger().warning("Floodgate not present! Bedrock player detection will be less precise.");
        }
    }

    public List<ResourcePack> getPacks(BackendServer server) {
        var list = this.packs.get(server.name());
        if (list == null) {
            getLogger().debug("No packs found for server " + server.name());
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * This is called early to set the new backend server, if we intend to switch it.
     */
    public Optional<BackendServer> handleFirst(BackendServer newServer, Channel channel, UUID uuid) {
        String xuid = checker.getBedrockXuid(channel, uuid);
        getLogger().debug("handleFirst xuid result: " + xuid);

        if (xuid == null) {
            getLogger().debug("Not a Bedrock player: " + uuid);
            return Optional.empty();
        }

        // Get the currently applied server packs
        BackendServer server = playerPackTracker.get(xuid);

        if (server == null) {
            // Should not happen? Would imply that someone slipped through the cracks
            // MAYBE, HUGE MAYBE, geyser standalone is used to connect but with geyser velocity installed...?
            getLogger().error(String.format("Player %s does not have any packs applied. Something is wrong here!", uuid));
        } else {
            getLogger().debug("player " + uuid + " has packs for server " + server.name());
        }

        BackendServer goal = inProgress.get(xuid);
        if (goal == null) {
            // Either first connection, or it's the start of switching servers; don't interfere
            getLogger().debug("Current goal server not set - likely first connection or server switch start");
            return Optional.empty();
        } else {
            getLogger().debug("Server goal known, setting early so other plugins can see it: " + goal.name());
        }

        // We want to switch to this backend server; let's send it
        return Optional.of(goal);
    }

    /**
     * This is called by the same event that uses {@link #handleFirst(BackendServer, Channel, UUID)}.
     * The goal here is to get the server that we are switching to - which may be changed by other plugins, and disconnect the player here.
     * This should *not* run at all for java players,
     */
    public Optional<BackendServer> handleLast(BackendServer targetServer, Channel channel, UUID uuid,
                              boolean firstConnection, Consumer<String> disconnectFunction) {
        String xuid = checker.getBedrockXuid(channel, uuid);
        getLogger().debug("handleLast xuid result: " + xuid);

        if (xuid == null) {
            getLogger().debug("Not a Bedrock player: " + uuid);
            return Optional.empty();
        }

        if (targetServer == null) {
            getLogger().error("How is the target null...?");
            return Optional.empty();
        }

        BackendServer currentServersPacks = playerPackTracker.get(xuid);
        BackendServer target = inProgress.get(xuid);

        getLogger().debug("Currently applied packs for server " + currentServersPacks.name());
        getLogger().debug("Current target: " + (target == null ? "not set" : target.name()));

        // Case 1: No goal server
        // a.) we want to start a switching goal, and need to re-connect for packs; or
        // b.) we didn't have a goal, but still got the right pack. Probably the default packs working :p
        if (target == null) {

            // All good, probably first connection with a "lucky" first server guess. Let's continue!
            if (targetServer.equals(currentServersPacks)) {
                getLogger().debug("Server result matches our current packs!");
                return Optional.empty();
            }

            // We can't disconnect a player error-free with no initial server; that freaks out the bungee/velocity api
            // Three options:
            // 1. Disconnect forcefully
            // 2. Forcefully connect to default server
            // 3. Do nothing?
            if (!firstConnection) {
                inProgress.put(xuid, targetServer);
                getLogger().debug("Connection " + uuid + " is switching to " + targetServer.name() + ". Transferring now...");
                GeyserApi.api().transfer(uuid, getConfig().getAddress(), getConfig().getPort());
                return Optional.empty();
            } else {
                if (getConfig().isKickOnMismatch()) {
                    disconnectFunction.accept(getConfig().getKickMessage());
                    return Optional.empty();
                }
                getLogger().warning("Player with uuid " + uuid + " is connecting to server " + targetServer.name()
                + " but has the packs for the server " + currentServersPacks.name() + ". Forcefully sending player " + uuid + " to that server.");
                return Optional.of(currentServersPacks);
            }
        }

        // Case 2: Server switch finished; we got our server :p
        if (target.equals(targetServer)) {
            inProgress.remove(xuid);
            if (targetServer.equals(currentServersPacks)) {
                getLogger().debug("Finished server switching & pack application for " + uuid + ". New server: " + target.name());
            } else {
                getLogger().error("Despite a seemingly successful reconnect to server " +
                        targetServer.name() + ", but the player has no resource packs. What happened!?");
            }
            return Optional.empty();
        }

        getLogger().error(String.format("Ran into weird state for player %s: " +
                "goal: %s, server sends to: %s, applied packs: %s", uuid, target.name(), targetServer.name(), currentServersPacks.name()));
        inProgress.remove(xuid);
        return Optional.of(currentServersPacks);
    }

    public void handleGeyserLoadResourcePackEvent(SessionLoadResourcePacksEvent event) {
        String xuid = event.connection().xuid();
        BackendServer server = inProgress.get(xuid);
        List<ResourcePack> packs;

        // Player is new, server not known; let's send them the default packs
        if (server == null) {
            getLogger().debug("Sending default server pack for " + event.connection().xuid());
            packs = getPacks(defaultServer);
            playerPackTracker.put(xuid, defaultServer);
        } else {
            getLogger().debug("Sending " + server.name() + " 's packs for " + event.connection().name());
            playerPackTracker.put(xuid, server);
            packs = getPacks(server);
        }

        packs.forEach(event::register);
    }

    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        getLogger().debug(String.format("User %s (%s) disconnected, removing from player pack tracker",
                event.connection().name(), event.connection().xuid()));
        this.playerPackTracker.remove(event.connection().xuid());
    }

    public void reload() {
        this.defaultServer = BOOTSTRAP.backendFromName(getConfig().getDefaultServer());
        this.packs.clear();

        this.packs = ResourcePackLoader.loadPacks(this);
    }

    public static PackSyncLogger getLogger() {
        return BOOTSTRAP.logger();
    }

    public PackSyncConfig getConfig() {
        return BOOTSTRAP.config();
    }

    public Path getDataFolder() {
        return BOOTSTRAP.dataFolder();
    }

    public BackendServer backendFromName(String name) {
        return BOOTSTRAP.backendFromName(name);
    }

}
