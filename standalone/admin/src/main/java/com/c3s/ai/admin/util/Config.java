package com.c3s.ai.admin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Config lookup: -Dkey, then $ENV_VAR, then db.properties on the classpath (file optional). */
public final class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final Config INSTANCE = new Config("db.properties");

    private final Properties file = new Properties();

    private Config(String classpathFile) {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(classpathFile)) {
            if (in != null) {
                file.load(in);
            } else {
                log.info("{} not on classpath; relying on system properties / environment", classpathFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + classpathFile, e);
        }
    }

    public String get(String key, String envName) {
        String v = System.getProperty(key);
        if (isBlank(v)) {
            v = System.getenv(envName);
        }
        if (isBlank(v)) {
            v = file.getProperty(key);
        }
        return isBlank(v) ? null : v.trim();
    }

    public String require(String key, String envName) {
        String v = get(key, envName);
        if (v == null) {
            throw new IllegalStateException("Missing required config: set -D" + key + ", " + envName
                    + ", or " + key + " in db.properties");
        }
        return v;
    }

    public String get(String key, String envName, String dflt) {
        String v = get(key, envName);
        return v == null ? dflt : v;
    }

    public int getInt(String key, String envName, int dflt) {
        String v = get(key, envName);
        try {
            return v == null ? dflt : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            log.warn("Invalid {}={}, using default {}", key, v, dflt);
            return dflt;
        }
    }

    public boolean getBoolean(String key, String envName, boolean dflt) {
        String v = get(key, envName);
        return v == null ? dflt : Boolean.parseBoolean(v);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
