package net.onebeastchris.geyserpacksync;

import net.onebeastchris.geyserpacksync.common.PSPLogger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public class LoggerImpl implements PSPLogger {

    private final Logger logger;

    private boolean debug = false;

    public LoggerImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.error(message, error);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        if (debug) logger.info("[DEBUG] " + message);
    }

    @Override
    public void debug(@Nullable Object object) {
        if (debug) logger.debug("[DEBUG]:", object);
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean isDebug() {
        return this.debug;
    }
}
