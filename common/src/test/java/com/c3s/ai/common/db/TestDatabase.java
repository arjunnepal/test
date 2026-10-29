package com.c3s.ai.common.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A throwaway MySQL database for DAO integration tests, shared by every test class in one JVM
 * (the connection pool in DBUtil is static, so there can only be one per JVM anyway).
 *
 * Configured with a server URL that has no database name:
 *   DONATION_IT_JDBC_URL='jdbc:mysql://localhost:3306/?sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8'
 *   DONATION_IT_JDBC_USER=root DONATION_IT_JDBC_PASSWORD=...
 *
 * start() creates uwnefl_it_&lt;hex&gt;, loads schema.sql and seed.sql, and points DBUtil at it; the
 * database is dropped when the JVM exits. Unconfigured, start() returns false and tests skip.
 * Published in this module's test-jar so browse, admin and report can use it.
 */
public final class TestDatabase {

    private static final String NAME = "uwnefl_it_" + Long.toHexString(System.nanoTime());

    private static Boolean started;
    private static String serverUrl;
    private static String user;
    private static String password;

    private TestDatabase() {
    }

    /** True when the database is ready; false when the env vars aren't set. */
    public static synchronized boolean start() throws Exception {
        if (started != null) {
            return started;
        }
        serverUrl = System.getenv("DONATION_IT_JDBC_URL");
        if (serverUrl == null || serverUrl.isBlank()) {
            started = false;
            return false;
        }
        user = System.getenv().getOrDefault("DONATION_IT_JDBC_USER", "root");
        password = System.getenv().getOrDefault("DONATION_IT_JDBC_PASSWORD", "");

        execute(serverUrl, "CREATE DATABASE " + NAME + " CHARACTER SET utf8mb4");
        Runtime.getRuntime().addShutdownHook(new Thread(TestDatabase::drop, "drop-" + NAME));

        String url = withDatabase(serverUrl, NAME);
        Path db = locateDbScripts();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            runScript(conn, db.resolve("schema.sql"));
            runScript(conn, db.resolve("seed.sql"));
        }

        // DBUtil reads its config on first use
        System.setProperty("jdbc.url", url);
        System.setProperty("jdbc.user", user);
        System.setProperty("jdbc.password", password);
        started = true;
        return true;
    }

    /** Run a statement directly (outside the DAOs) - for arranging test data. */
    public static void exec(String sql) throws SQLException {
        execute(withDatabase(serverUrl, NAME), sql);
    }

    private static void drop() {
        try {
            DBUtil.shutdown();
        } catch (Throwable ignored) {
            // pool may never have been used
        }
        try {
            execute(serverUrl, "DROP DATABASE IF EXISTS " + NAME);
        } catch (SQLException e) {
            System.err.println("Could not drop test database " + NAME + ": " + e.getMessage());
        }
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // jdbc:mysql://host:3306/?params -> jdbc:mysql://host:3306/<db>?params
    private static String withDatabase(String url, String database) {
        int query = url.indexOf('?');
        String base = query < 0 ? url : url.substring(0, query);
        String params = query < 0 ? "" : url.substring(query);
        return (base.endsWith("/") ? base : base + "/") + database + params;
    }

    // Surefire runs in each module's directory; the scripts live in common/.
    private static Path locateDbScripts() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("common/src/main/resources/db");
            if (Files.isRegularFile(candidate.resolve("schema.sql"))) {
                return candidate;
            }
            Path local = dir.resolve("src/main/resources/db");
            if (Files.isRegularFile(local.resolve("schema.sql"))) {
                return local;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not find common/src/main/resources/db/schema.sql");
    }

    // each statement ends with ";" at the end of a line
    private static void runScript(Connection conn, Path script) throws Exception {
        StringBuilder statement = new StringBuilder();
        try (Statement stmt = conn.createStatement()) {
            for (String line : Files.readAllLines(script)) {
                statement.append(line).append('\n');
                if (line.stripTrailing().endsWith(";")) {
                    String sql = statement.toString().strip();
                    statement.setLength(0);
                    stmt.execute(sql.substring(0, sql.length() - 1));
                }
            }
        }
    }
}
