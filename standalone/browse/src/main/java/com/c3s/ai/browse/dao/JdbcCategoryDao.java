package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.exception.DataAccessException;
import com.c3s.ai.browse.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcCategoryDao implements CategoryDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcCategoryDao.class);

    private static final String SELECT_IN_USE_SQL = """
            SELECT c.id, c.label, c.emoji, c.color_class
            FROM uwnefl_categories c
            WHERE c.status = 'active'
              AND EXISTS (SELECT 1
                          FROM uwnefl_profile_categories pc
                          JOIN uwnefl_profiles p ON p.id = pc.profile_id AND p.status = 'published'
                          WHERE pc.category_id = c.id)
            ORDER BY c.id ASC
            """;

    @Override
    public List<Category> findInUse() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_IN_USE_SQL);
             ResultSet rs = stmt.executeQuery()) {
            List<Category> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            log.error("Failed to load categories in use", e);
            throw new DataAccessException("Failed to load categories in use", e);
        }
    }

    // also used by JdbcBrowseProfileDao for a card's categories
    static Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setLabel(rs.getString("label"));
        c.setEmoji(rs.getString("emoji"));
        c.setColorClass(rs.getString("color_class"));
        return c;
    }
}
