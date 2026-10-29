package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.db.DBUtil;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.exception.DataAccessException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.CategoryStatus;
import com.c3s.ai.admin.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcCategoryDao implements CategoryDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcCategoryDao.class);

    private static final String SELECT_ALL_SQL = "SELECT * FROM uwnefl_categories ORDER BY label ASC";
    private static final String SELECT_ACTIVE_SQL = "SELECT * FROM uwnefl_categories WHERE status = 'active' ORDER BY label ASC";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM uwnefl_categories WHERE id = ?";
    private static final String INSERT_SQL =
            "INSERT INTO uwnefl_categories (label, emoji, color_class, status) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE uwnefl_categories SET label = ?, emoji = ?, color_class = ?, status = ? WHERE id = ?";
    private static final String ARCHIVE_SQL = "UPDATE uwnefl_categories SET status = 'archived' WHERE id = ?";
    private static final String LABEL_EXISTS_SQL = "SELECT 1 FROM uwnefl_categories WHERE label = ? LIMIT 1";
    private static final String LABEL_EXISTS_EXCLUDING_SQL =
            "SELECT 1 FROM uwnefl_categories WHERE label = ? AND id <> ? LIMIT 1";

    @Override
    public List<Category> findAll(boolean activeOnly) {
        String sql = activeOnly ? SELECT_ACTIVE_SQL : SELECT_ALL_SQL;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
            return categories;
        } catch (SQLException e) {
            log.error("Failed to load category list", e);
            throw new DataAccessException("Failed to load category list", e);
        }
    }

    @Override
    public Category findById(long id) throws EntityNotFoundException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntityNotFoundException("category", id);
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            log.error("Failed to load category id={}", id, e);
            throw new DataAccessException("Failed to load category id=" + id, e);
        }
    }

    @Override
    public boolean labelExists(String label, Long excludeId) {
        String sql = excludeId != null ? LABEL_EXISTS_EXCLUDING_SQL : LABEL_EXISTS_SQL;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, label);
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check category label uniqueness for value={}", Log.clean(label), e);
            throw new DataAccessException("Failed to check category label uniqueness", e);
        }
    }

    @Override
    public long insert(Category category) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getLabel());
            stmt.setString(2, category.getEmoji());
            stmt.setString(3, category.getColorClass());
            stmt.setString(4, category.getStatus().dbValue());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert did not return a generated id");
                }
                long id = keys.getLong(1);
                log.debug("Inserted category id={}", id);
                return id;
            }
        } catch (SQLException e) {
            log.error("Failed to insert category label={}", Log.clean(category.getLabel()), e);
            throw new DataAccessException("Failed to insert category", e);
        }
    }

    @Override
    public void update(Category category) throws EntityNotFoundException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, category.getLabel());
            stmt.setString(2, category.getEmoji());
            stmt.setString(3, category.getColorClass());
            stmt.setString(4, category.getStatus().dbValue());
            stmt.setLong(5, category.getId());
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new EntityNotFoundException("category", category.getId());
            }
            log.debug("Updated category id={}", category.getId());
        } catch (SQLException e) {
            log.error("Failed to update category id={}", category.getId(), e);
            throw new DataAccessException("Failed to update category id=" + category.getId(), e);
        }
    }

    @Override
    public void archive(long id) throws EntityNotFoundException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ARCHIVE_SQL)) {
            stmt.setLong(1, id);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new EntityNotFoundException("category", id);
            }
            log.debug("Archived category id={}", id);
        } catch (SQLException e) {
            log.error("Failed to archive category id={}", id, e);
            throw new DataAccessException("Failed to archive category id=" + id, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setLabel(rs.getString("label"));
        c.setEmoji(rs.getString("emoji"));
        c.setColorClass(rs.getString("color_class"));
        c.setStatus(CategoryStatus.fromDbValue(rs.getString("status")));
        return c;
    }
}
