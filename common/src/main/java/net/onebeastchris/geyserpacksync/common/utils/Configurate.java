package net.onebeastchris.geyserpacksync.common.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import net.onebeastchris.geyserpacksync.common.PSPLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Configurate {

    /**
     * Load config
     *
     * @param dataDirectory The config's directory
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Configurate create(Path dataDirectory) {
        File folder = dataDirectory.toFile();
        File file = new File(folder, "config.yml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (InputStream input = Configurate.class.getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                    .disable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
            return mapper.readValue(dataDirectory.resolve("config.yml").toFile(), Configurate.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create GeyserPackSync config!", e);
        }
    }

    @JsonProperty("port")
    @Getter
    int port;

    @JsonProperty("address")
    @Getter
    String address;

    @JsonProperty("servers")
    @Getter
    private List<Server> servers;

    @JsonProperty("default-server")
    @Getter
    String defaultServer;

    @JsonProperty("debug")
    @Getter
    boolean debug;

    @JsonProperty("kick-on-mismatch")
    @Getter
    boolean kickOnMismatch;

    @JsonProperty("kick-message")
    @Getter
    String kickMessage;
    public record Server(@NonNull @JsonProperty("name") String name,
                         @Nullable @JsonProperty("forced-host") String forcedHost) {
    }

    public static boolean checkConfig(PSPLogger logger, Configurate config) {
        if (config == null) {
            logger.error("Config is null! Please check your config.yml. Regenerating it fully might help.");
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

        if (config.getServers() == null || config.getServers().isEmpty()) {
            logger.warning("No servers are currently defined, so GeyserPackSync will not work. Check out the GeyserPackSync config!");
        }

        if (config.getDefaultServer() == null || config.getDefaultServer().isEmpty()) {
            logger.warning("No default server is currently defined! Please set one in the GeyserPackSync config!");
        }

        return true;
    }
}
