package com.c3s.ai.common.db;

import com.c3s.ai.common.util.DbEnums;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Statement plumbing on an already-open connection. Package-private: reach it through JdbcReader/JdbcWriter. */
final class Sql {

    private Sql() {
    }

    static <T> List<T> list(Connection conn, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            List<T> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapper.map(rs));
            }
            return out;
        }
    }

    static <T> Optional<T> optional(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
        }
    }

    /** First column of the first row as a long; no row or SQL NULL -> 0. */
    static long scalarLong(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    static boolean exists(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        }
    }

    static int update(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepare(conn, sql, params)) {
            return stmt.executeUpdate();
        }
    }

    static long insertReturningKey(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, params);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert did not return a generated id");
                }
                return keys.getLong(1);
            }
        }
    }

    static <T> void batch(Connection conn, String sql, Collection<T> rows, Function<T, Object[]> params)
            throws SQLException {
        if (rows.isEmpty()) {
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (T row : rows) {
                bind(stmt, params.apply(row));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private static PreparedStatement prepare(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        try {
            bind(stmt, params);
            return stmt;
        } catch (SQLException | RuntimeException e) {
            stmt.close();
            throw e;
        }
    }

    /** Positional binding. Enums are stored as their lower-case name; LocalDate as DATE. */
    private static void bind(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            int idx = i + 1;
            Object p = params[i];
            if (p == null) {
                stmt.setNull(idx, Types.NULL);
            } else if (p instanceof String s) {
                stmt.setString(idx, s);
            } else if (p instanceof Integer n) {
                stmt.setInt(idx, n);
            } else if (p instanceof Long n) {
                stmt.setLong(idx, n);
            } else if (p instanceof Boolean b) {
                stmt.setBoolean(idx, b);
            } else if (p instanceof BigDecimal d) {
                stmt.setBigDecimal(idx, d);
            } else if (p instanceof LocalDate d) {
                stmt.setObject(idx, d);
            } else if (p instanceof Enum<?> e) {
                stmt.setString(idx, DbEnums.toDb(e));
            } else {
                throw new IllegalArgumentException("Unsupported SQL parameter type: " + p.getClass().getName());
            }
        }
    }
}
