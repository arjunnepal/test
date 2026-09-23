package com.c3s.ai.common.db;

import com.c3s.ai.common.config.Config;
import com.c3s.ai.common.exception.ConfigurationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The one HikariCP pool, built at class-init from Config. Connections are handed out inside this
 * package only, so no feature module can open one and run arbitrary SQL.
 */
public final class DBUtil {

    private static final Logger log = LoggerFactory.getLogger(DBUtil.class);

    private static final HikariDataSource dataSource;

    static {
        Config cfg = Config.INSTANCE;
        String url = cfg.require("jdbc.url", "JDBC_URL");

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("donation-pool");
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
        // -1: build the pool now but don't open a connection until first use, so a DB
        // outage at startup doesn't kill this class - the pool reconnects on its own.
        hikari.setInitializationFailTimeout(-1);

        try {
            dataSource = new HikariDataSource(hikari);
        } catch (RuntimeException e) {
            throw new ConfigurationException(
                    "Failed to initialize the database connection pool for " + redactJdbcUrl(url), e);
        }
        log.info("DB pool initialized for {}", redactJdbcUrl(url));
    }

    private DBUtil() {
    }

    /** Scheme + host + port + database only - drops any user:pass@ userinfo and the query string. */
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

    static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Runs SELECT 1. Never throws - a failure is logged at WARN and returns false. */
    public static boolean ping(int queryTimeoutSeconds) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(queryTimeoutSeconds);
            stmt.execute("SELECT 1");
            return true;
        } catch (SQLException | RuntimeException e) {
            log.warn("Database ping failed: {}", e.getMessage());
            return false;
        }
    }

    // called on undeploy so a redeploy doesn't leak connections/threads
    public static void shutdown() {
        dataSource.close();
        log.info("DB pool closed");
    }
}
