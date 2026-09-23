package com.c3s.ai.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Maps the current row of a ResultSet. Must not call next(). */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
