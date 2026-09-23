package com.c3s.ai.common.db;

import java.sql.Connection;
import java.sql.SQLException;

final class Transactions {

    private Transactions() {
    }

    /** Roll back, attaching a rollback failure to the original error instead of hiding it. */
    static void rollbackQuietly(Connection conn, Throwable original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
