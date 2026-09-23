package com.c3s.ai.common.db;

import com.c3s.ai.common.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Base for read-only DAOs: queries only, so a DAO extending it has no way to write.
 * {@link #read(String, ReadWork)} runs several queries on one connection, so they see one
 * snapshot. SQLException becomes DataAccessException; the servlet that catches it logs it.
 */
public abstract class JdbcReader {

    @FunctionalInterface
    protected interface ReadWork<T> {
        T run(Reads reads) throws SQLException;
    }

    protected final <T> List<T> queryList(String what, String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.list(conn, sql, mapper, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    protected final <T> Optional<T> queryOptional(String what, String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.optional(conn, sql, mapper, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    protected final long queryLong(String what, String sql, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.scalarLong(conn, sql, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    protected final boolean exists(String what, String sql, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.exists(conn, sql, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    /** Several reads on one connection, in a read-only transaction (one consistent snapshot). */
    protected final <T> T read(String what, ReadWork<T> work) {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);
            try {
                T result = work.run(new Reads(conn));
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                Transactions.rollbackQuietly(conn, e);
                throw e;
            }
            // Hikari restores autoCommit/readOnly when the connection goes back to the pool
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    DataAccessException translate(String what, SQLException e) {
        return new DataAccessException(what + " failed", e);
    }

    /** Query operations bound to one connection. Only JdbcReader / JdbcWriter create these. */
    public static class Reads {

        final Connection conn;

        Reads(Connection conn) {
            this.conn = conn;
        }

        public <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
            return Sql.list(conn, sql, mapper, params);
        }

        public <T> Optional<T> optional(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
            return Sql.optional(conn, sql, mapper, params);
        }

        public long scalarLong(String sql, Object... params) throws SQLException {
            return Sql.scalarLong(conn, sql, params);
        }

        public boolean exists(String sql, Object... params) throws SQLException {
            return Sql.exists(conn, sql, params);
        }
    }
}
