package com.c3s.ai.common.db;

import com.c3s.ai.common.exception.ConstraintViolationException;
import com.c3s.ai.common.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Function;

/**
 * Base for DAOs that write: adds updates, inserts, batches and transactions to JdbcReader.
 * A write refused by an integrity constraint surfaces as ConstraintViolationException, so a
 * service can report it as a form error instead of a 500.
 */
public abstract class JdbcWriter extends JdbcReader {

    @FunctionalInterface
    protected interface WriteWork<T> {
        T run(Writes writes) throws SQLException;
    }

    protected final int update(String what, String sql, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.update(conn, sql, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    protected final long insertReturningKey(String what, String sql, Object... params) {
        try (Connection conn = DBUtil.getConnection()) {
            return Sql.insertReturningKey(conn, sql, params);
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    /**
     * Commit on success, roll back on any failure - including an Error - and rethrow.
     * Autocommit is left to the pool to restore, so a failed reset can't mask the real outcome.
     */
    protected final <T> T inTransaction(String what, WriteWork<T> work) {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(new Writes(conn));
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                Transactions.rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw translate(what, e);
        }
    }

    @Override
    DataAccessException translate(String what, SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) {
            return new ConstraintViolationException(what + " violated a database constraint", e);
        }
        return super.translate(what, e);
    }

    /** Read and write operations bound to one transaction's connection. */
    public static final class Writes extends Reads {

        Writes(Connection conn) {
            super(conn);
        }

        public int update(String sql, Object... params) throws SQLException {
            return Sql.update(conn, sql, params);
        }

        public long insertReturningKey(String sql, Object... params) throws SQLException {
            return Sql.insertReturningKey(conn, sql, params);
        }

        public <T> void batch(String sql, Collection<T> rows, Function<T, Object[]> params) throws SQLException {
            Sql.batch(conn, sql, rows, params);
        }
    }
}
