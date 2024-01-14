package net.onebeastchris.geyserpacksync;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.onebeastchris.geyserpacksync.common.utils.BackendServer;

import java.util.Objects;

public class VelocityBackendServer implements BackendServer {

    private final RegisteredServer registeredServer;

    @Override
    public String name() {
        return registeredServer.getServerInfo().getName();
    }

    public RegisteredServer registeredServer() {
        return registeredServer;
    }

    private VelocityBackendServer(RegisteredServer registeredServer) {
        this.registeredServer = registeredServer;
    }

    public static VelocityBackendServer of(RegisteredServer registeredServer) {
        return new VelocityBackendServer(registeredServer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VelocityBackendServer compare) {
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
