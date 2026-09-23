package com.c3s.ai.core.user;

import com.c3s.ai.common.db.JdbcReader;

import java.util.Optional;

final class JdbcUserCredentialsDao extends JdbcReader implements UserCredentialsDao {

    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT id, username, password_hash, display_name, role FROM uwnefl_users WHERE username = ?";

    @Override
    public Optional<UserCredentials> findByUsername(String username) {
        return queryOptional("Load user credentials", SELECT_BY_USERNAME_SQL, rs -> new UserCredentials(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                Role.fromDb(rs.getString("role"))), username);
    }
}
