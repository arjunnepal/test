package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.db.DBUtil;
import com.c3s.ai.admin.exception.DataAccessException;
import com.c3s.ai.admin.model.User;
import com.c3s.ai.admin.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserDao.class);

    private static final String SELECT_BY_USERNAME_SQL = "SELECT * FROM uwnefl_users WHERE username = ?";

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to load user username={}", Log.clean(username), e);
            throw new DataAccessException("Failed to load user", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    }
}
