package com.c3s.ai.browse.db;

import com.c3s.ai.browse.exception.ConfigurationException;
import com.c3s.ai.browse.util.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public final class DBUtil {

    private static final Logger log = LoggerFactory.getLogger(DBUtil.class);

    private static final HikariDataSource dataSource;

    static {
        Config cfg = Config.INSTANCE;
        String url = cfg.require("jdbc.url", "JDBC_URL");

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("browse-pool");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(url);
        hikari.setUsername(cfg.require("jdbc.user", "JDBC_USER"));
        hikari.setPassword(cfg.require("jdbc.password", "JDBC_PASSWORD"));
        hikari.setMaximumPoolSize(cfg.getInt("jdbc.pool.maxSize", "JDBC_POOL_MAX_SIZE", 10));
        hikari.setMinimumIdle(cfg.getInt("jdbc.pool.minIdle", "JDBC_POOL_MIN_IDLE", 2));
        hikari.setConnectionTimeout(10_000);
        hikari.setValidationTimeout(3_000);
        hikari.setKeepaliveTime(60_000);
        hikari.setIdleTimeout(300_000);
        hikari.setMaxLifetime(1_800_000);
        // the weekly/monthly queries use NOW(), so keep them in UTC whatever the server's zone is
        hikari.setConnectionInitSql("SET time_zone = '+00:00'");
        // start even if the DB is down
        hikari.setInitializationFailTimeout(-1);

        try {
            dataSource = new HikariDataSource(hikari);
        } catch (RuntimeException e) {
            throw new ConfigurationException(
                    "Failed to initialize the database connection pool for " + redactJdbcUrl(url), e);
        }
        log.info("DB pool initialized for {}", redactJdbcUrl(url));
    }

    // strip credentials before logging
    static String redactJdbcUrl(String url) {
        if (url == null) {
            return "null";
        }
        int query = url.indexOf('?');
        String base = query < 0 ? url : url.substring(0, query);
        int at = base.indexOf('@');
        if (at < 0) {
            return base;
        }
        int schemeEnd = base.indexOf("://");
        String prefix = schemeEnd < 0 ? "" : base.substring(0, schemeEnd + 3);
        return prefix + base.substring(at + 1);
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        dataSource.close();
        log.info("DB pool closed");
    }
}
