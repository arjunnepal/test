package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.exception.DataAccessException;
import com.c3s.ai.browse.model.User;
import com.c3s.ai.browse.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserDao.class);

    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT username, password_hash, display_name FROM uwnefl_browse_users WHERE username = ?";

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setDisplayName(rs.getString("display_name"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            log.error("Failed to load user username={}", Log.clean(username), e);
            throw new DataAccessException("Failed to load user", e);
        }
    }
}
