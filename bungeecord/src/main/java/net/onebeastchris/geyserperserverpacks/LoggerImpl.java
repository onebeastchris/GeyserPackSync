package net.onebeastchris.geyserperserverpacks;

import net.onebeastchris.geyserperserverpacks.common.PSPLogger;

import java.util.logging.Logger;

public class LoggerImpl implements PSPLogger {

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
            bungeeLogger.info(message);
        }
    }

    @Override
    public void debug(Object object) {
        if (debug) {
            PSPLogger.super.debug(object);
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
