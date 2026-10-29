package com.c3s.ai.admin.exception;

/** Required config (DB pool, upload dir, ...) couldn't be set up at startup. */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
