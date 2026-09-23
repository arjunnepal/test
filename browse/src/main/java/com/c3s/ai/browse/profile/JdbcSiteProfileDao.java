package com.c3s.ai.browse.profile;

import com.c3s.ai.common.db.JdbcReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JdbcSiteProfileDao extends JdbcReader implements SiteProfileDao {

    // oldest first ("Featured")
    private static final String SELECT_PUBLISHED_SQL = """
            SELECT id, name, blurb, emoji, goal, raised
            FROM uwnefl_profiles
            WHERE status = 'published'
            ORDER BY created_at ASC, id ASC
            """;

    // Active categories only, by label - the same order the dashboard uses, so a profile in
    // several categories gets the same first category, and colour, on both sides.
    private static final String SELECT_PUBLISHED_CATEGORIES_SQL = """
            SELECT pc.profile_id, c.id, c.label, c.emoji, c.color_class
            FROM uwnefl_profile_categories pc
            JOIN uwnefl_categories c ON c.id = pc.category_id AND c.status = 'active'
            JOIN uwnefl_profiles p ON p.id = pc.profile_id AND p.status = 'published'
            ORDER BY c.label ASC
            """;

    private static final String PUBLISHED_TOTALS_SQL =
            "SELECT COUNT(*), COALESCE(SUM(raised), 0) FROM uwnefl_profiles WHERE status = 'published'";

    // only active categories with a published profile, in creation order
    private static final String RAISED_BY_CATEGORY_SQL = """
            SELECT c.label, c.emoji, c.color_class, SUM(p.raised) AS raised
            FROM uwnefl_categories c
            JOIN uwnefl_profile_categories pc ON pc.category_id = c.id
            JOIN uwnefl_profiles p ON p.id = pc.profile_id AND p.status = 'published'
            WHERE c.status = 'active'
            GROUP BY c.id, c.label, c.emoji, c.color_class
            ORDER BY c.id ASC
            """;

    // ties go to the older profile
    private static final String TOP_WEEKLY_SQL = """
            SELECT d.profile_id, SUM(d.amount) AS amt
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            GROUP BY d.profile_id
            ORDER BY amt DESC, d.profile_id ASC
            LIMIT ?
            """;

    private record Row(long id, String name, String blurb, String emoji, int goal, int raised) {
    }

    @Override
    public List<PublishedProfile> findPublished() {
        // both queries in one read-only transaction, so a profile published or archived between
        // them can't turn up without its categories
        return read("Load published profiles", r -> {
            Map<Long, Row> rows = new LinkedHashMap<>();
            for (Row row : r.list(SELECT_PUBLISHED_SQL, rs -> new Row(rs.getLong("id"), rs.getString("name"),
                    rs.getString("blurb"), rs.getString("emoji"), rs.getInt("goal"), rs.getInt("raised")))) {
                rows.put(row.id(), row);
            }
            Map<Long, List<CardCategory>> categories = new HashMap<>();
            r.list(SELECT_PUBLISHED_CATEGORIES_SQL, rs -> {
                categories.computeIfAbsent(rs.getLong("profile_id"), k -> new ArrayList<>())
                        .add(new CardCategory(rs.getLong("id"), rs.getString("label"),
                                rs.getString("emoji"), rs.getString("color_class")));
                return null;
            });
            List<PublishedProfile> out = new ArrayList<>(rows.size());
            for (Row row : rows.values()) {
                out.add(new PublishedProfile(row.id(), row.name(), row.blurb(), row.emoji(), row.goal(), row.raised(),
                        categories.getOrDefault(row.id(), List.of())));
            }
            return out;
        });
    }

    @Override
    public PublishedTotals publishedTotals() {
        return queryOptional("Load published totals", PUBLISHED_TOTALS_SQL,
                rs -> new PublishedTotals(rs.getLong(1), rs.getLong(2)))
                .orElse(new PublishedTotals(0, 0));
    }

    @Override
    public List<CategoryRaised> raisedByCategory() {
        return queryList("Load raised-by-category totals", RAISED_BY_CATEGORY_SQL, rs -> new CategoryRaised(
                rs.getString("label"), rs.getString("emoji"), rs.getString("color_class"), rs.getLong("raised")));
    }

    @Override
    public Map<Long, Long> topWeeklyRaised(int limit) {
        Map<Long, Long> out = new HashMap<>();
        queryList("Load top weekly profiles", TOP_WEEKLY_SQL, rs -> {
            out.put(rs.getLong("profile_id"), rs.getLong("amt"));
            return null;
        }, Math.max(1, limit));
        return out;
    }
}
