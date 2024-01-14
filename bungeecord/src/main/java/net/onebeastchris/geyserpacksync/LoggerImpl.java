package net.onebeastchris.geyserpacksync;

import net.onebeastchris.geyserpacksync.common.utils.PackSyncLogger;

import java.util.logging.Logger;

public class LoggerImpl implements PackSyncLogger {

    Logger bungeeLogger;
    boolean debug = false;

    public LoggerImpl(Logger bungeeLogger) {
        this.bungeeLogger = bungeeLogger;
    }

    @Override
    public void error(String message) {
        bungeeLogger.severe(message);
    }

    @Override
    public void error(String message, Throwable error) {
        bungeeLogger.severe(message);
        bungeeLogger.severe(error.getMessage());
    }

    @Override
    public void warning(String message) {
        bungeeLogger.warning(message);
    }

    @Override
    public void info(String message) {
        bungeeLogger.info(message);
    }

    @Override
    public void debug(String message) {
        if (debug) {
            bungeeLogger.info("[DEBUG]: " + message);
        }
    }

    @Override
    public void debug(Object object) {
        if (debug) {
            bungeeLogger.info("[DEBUG]:" + object);
        }
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }
}
