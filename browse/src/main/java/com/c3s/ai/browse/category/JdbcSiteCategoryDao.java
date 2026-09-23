package com.c3s.ai.browse.category;

import com.c3s.ai.common.db.JdbcReader;

import java.util.List;

final class JdbcSiteCategoryDao extends JdbcReader implements SiteCategoryDao {

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
    public List<ChipCategory> findInUse() {
        return queryList("Load categories in use", SELECT_IN_USE_SQL, rs -> new ChipCategory(
                rs.getLong("id"), rs.getString("label"), rs.getString("emoji"), rs.getString("color_class")));
    }
}
