package net.onebeastchris.geyserpacksync;

import net.md_5.bungee.api.config.ServerInfo;
import net.onebeastchris.geyserpacksync.common.utils.BackendServer;

import java.util.Objects;

public class BungeeBackendServer implements BackendServer {

    private final ServerInfo serverInfo;

    @Override
    public String name() {
        return serverInfo.getName();
    }

    public ServerInfo info() {
        return serverInfo;
    }

    private BungeeBackendServer(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public static BungeeBackendServer of(ServerInfo serverInfo) {
        return new BungeeBackendServer(serverInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BungeeBackendServer compare) {
            return Objects.equals(compare.name(), this.name());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // We don't care about the ServerInfo object being the same
        return name().hashCode();
    }
}
