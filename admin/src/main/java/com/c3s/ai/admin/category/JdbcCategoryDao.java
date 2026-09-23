package com.c3s.ai.admin.category;

import com.c3s.ai.common.db.JdbcWriter;
import com.c3s.ai.common.db.RowMapper;

import java.util.List;
import java.util.Optional;

final class JdbcCategoryDao extends JdbcWriter implements CategoryDao {

    private static final String COLUMNS = "id, label, emoji, color_class, status";

    private static final String SELECT_ALL_SQL = "SELECT " + COLUMNS + " FROM uwnefl_categories ORDER BY label ASC";
    private static final String SELECT_ACTIVE_REFS_SQL =
            "SELECT id, label, emoji, color_class FROM uwnefl_categories WHERE status = 'active' ORDER BY label ASC";
    private static final String SELECT_BY_ID_SQL = "SELECT " + COLUMNS + " FROM uwnefl_categories WHERE id = ?";
    private static final String SELECT_LABEL_SQL = "SELECT label FROM uwnefl_categories WHERE id = ?";
    private static final String LABEL_EXISTS_SQL = "SELECT 1 FROM uwnefl_categories WHERE label = ? LIMIT 1";
    private static final String LABEL_EXISTS_EXCLUDING_SQL =
            "SELECT 1 FROM uwnefl_categories WHERE label = ? AND id <> ? LIMIT 1";
    private static final String INSERT_SQL =
            "INSERT INTO uwnefl_categories (label, emoji, color_class, status) VALUES (?, ?, ?, ?)";
    // the status condition makes an archived row impossible to edit, however the request got here
    private static final String UPDATE_ACTIVE_SQL =
            "UPDATE uwnefl_categories SET label = ?, emoji = ?, color_class = ? WHERE id = ? AND status = 'active'";
    private static final String ARCHIVE_SQL = "UPDATE uwnefl_categories SET status = 'archived' WHERE id = ?";

    private static final RowMapper<Category> CATEGORY = rs -> {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setLabel(rs.getString("label"));
        c.setEmoji(rs.getString("emoji"));
        c.setColorClass(rs.getString("color_class"));
        c.setStatus(CategoryStatus.fromDbValue(rs.getString("status")));
        return c;
    };

    static final RowMapper<CategoryRef> REF = rs -> new CategoryRef(
            rs.getLong("id"), rs.getString("label"), rs.getString("emoji"), rs.getString("color_class"));

    @Override
    public List<Category> findAll() {
        return queryList("Load category list", SELECT_ALL_SQL, CATEGORY);
    }

    @Override
    public List<CategoryRef> findActiveRefs() {
        return queryList("Load active categories", SELECT_ACTIVE_REFS_SQL, REF);
    }

    @Override
    public Optional<Category> findById(long id) {
        return queryOptional("Load category", SELECT_BY_ID_SQL, CATEGORY, id);
    }

    @Override
    public Optional<String> findLabel(long id) {
        return queryOptional("Load category label", SELECT_LABEL_SQL, rs -> rs.getString(1), id);
    }

    @Override
    public boolean labelExists(String label, Long excludeId) {
        return excludeId == null
                ? exists("Check category label", LABEL_EXISTS_SQL, label)
                : exists("Check category label", LABEL_EXISTS_EXCLUDING_SQL, label, excludeId);
    }

    @Override
    public long insert(Category category) {
        return insertReturningKey("Insert category", INSERT_SQL,
                category.getLabel(), category.getEmoji(), category.getColorClass(), category.getStatus());
    }

    @Override
    public boolean updateActive(Category category) {
        return update("Update category", UPDATE_ACTIVE_SQL,
                category.getLabel(), category.getEmoji(), category.getColorClass(), category.getId()) > 0;
    }

    @Override
    public boolean archive(long id) {
        return update("Archive category", ARCHIVE_SQL, id) > 0;
    }
}
