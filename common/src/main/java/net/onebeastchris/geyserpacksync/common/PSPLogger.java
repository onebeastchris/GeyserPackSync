package net.onebeastchris.geyserpacksync.common;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface PSPLogger {

    /**
     * Logs an error message to console
     *
     * @param message the message to log
     */
    void error(String message);

    /**
     * Logs an error message and an exception to console
     *
     * @param message the message to log
     * @param error the error to throw
     */
    void error(String message, Throwable error);

    /**
     * Logs a warning message to console
     *
     * @param message the message to log
     */
    void warning(String message);

    /**
     * Logs an info message to console
     *
     * @param message the message to log
     */
    void info(String message);

    /**
     * Logs a debug message to console
     *
     * @param message the message to log
     */
    void debug(String message);

    /**
     * Logs an object to console if debug mode is enabled
     *
     * @param object the object to log
     */
    default void debug(@Nullable Object object) {
        debug(String.valueOf(object));
    }

    /**
     * Sets if the logger should print debug messages
     *
     * @param debug if the logger should print debug messages
     */
    void setDebug(boolean debug);

    /**
     * If debug is enabled for this logger
     */
    boolean isDebug();
}
