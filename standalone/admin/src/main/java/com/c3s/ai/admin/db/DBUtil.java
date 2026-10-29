package com.c3s.ai.admin.db;

import com.c3s.ai.admin.exception.ConfigurationException;
import com.c3s.ai.admin.util.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/** The one HikariCP pool for the app, built at class-init from Config. */
public final class DBUtil {

    private static final Logger log = LoggerFactory.getLogger(DBUtil.class);

    private static final HikariDataSource dataSource;

    static {
        Config cfg = Config.INSTANCE;
        String url = cfg.require("jdbc.url", "JDBC_URL");

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("admin-pool");
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

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // called on undeploy so a redeploy doesn't leak connections/threads
    public static void shutdown() {
        dataSource.close();
        log.info("DB pool closed");
    }

    @FunctionalInterface
    public interface SqlAction<T> {
        T run(Connection conn) throws SQLException;
    }

    /** Run action in one transaction. Commit on success, roll back + rethrow on any failure. */
    public static <T> T inTransaction(SqlAction<T> action) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = action.run(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                rollbackQuietly(conn, e);
                throw e;
            } catch (RuntimeException e) {
                rollbackQuietly(conn, e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // NOTE: setAutoCommit(true) in the finally above will commit a partial write unless we've
    // rolled back first, so the RuntimeException path has to roll back too - not just SQLException.
    private static void rollbackQuietly(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
