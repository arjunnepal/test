package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.exception.DataAccessException;
import com.c3s.ai.browse.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcBrowseProfileDao implements BrowseProfileDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcBrowseProfileDao.class);

    // oldest first ("Featured")
    private static final String SELECT_PUBLISHED_SQL = """
            SELECT id, name, blurb, emoji, goal, raised
            FROM uwnefl_profiles
            WHERE status = 'published'
            ORDER BY created_at ASC, id ASC
            """;

    // by category id, so a card's tag follows the same order as the chips
    private static final String SELECT_PUBLISHED_CATEGORIES_SQL = """
            SELECT pc.profile_id, c.id, c.label, c.emoji, c.color_class
            FROM uwnefl_profile_categories pc
            JOIN uwnefl_categories c ON c.id = pc.category_id
            JOIN uwnefl_profiles p ON p.id = pc.profile_id AND p.status = 'published'
            ORDER BY c.id ASC
            """;

    private static final String PUBLISHED_TOTALS_SQL =
            "SELECT COUNT(*) AS c, COALESCE(SUM(raised), 0) AS r "
                    + "FROM uwnefl_profiles WHERE status = 'published'";

    // only categories with a published profile
    private static final String RAISED_BY_CATEGORY_SQL = """
            SELECT c.id, c.label, c.emoji, c.color_class, SUM(p.raised) AS raised
            FROM uwnefl_categories c
            JOIN uwnefl_profile_categories pc ON pc.category_id = c.id
            JOIN uwnefl_profiles p ON p.id = pc.profile_id AND p.status = 'published'
            WHERE c.status = 'active'
            GROUP BY c.id, c.label, c.emoji, c.color_class
            ORDER BY c.id ASC
            """;

    // ties go to the older profile
    private static final String TOP_WEEKLY_SQL = """
            SELECT d.profile_id AS pid, SUM(d.amount) AS amt
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            GROUP BY d.profile_id
            ORDER BY amt DESC, d.profile_id ASC
            LIMIT ?
            """;

    @Override
    public List<Profile> findPublishedWithCategories() {
        try (Connection conn = DBUtil.getConnection()) {
            Map<Long, Profile> byId = new LinkedHashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLISHED_SQL);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Profile p = mapRow(rs);
                    byId.put(p.getId(), p);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLISHED_CATEGORIES_SQL);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // published between the two queries
                    Profile p = byId.get(rs.getLong("profile_id"));
                    if (p != null) {
                        p.getCategories().add(JdbcCategoryDao.mapRow(rs));
                    }
                }
            }
            return new ArrayList<>(byId.values());
        } catch (SQLException e) {
            log.error("Failed to load published profiles", e);
            throw new DataAccessException("Failed to load published profiles", e);
        }
    }

    @Override
    public PublishedTotals publishedTotals() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(PUBLISHED_TOTALS_SQL);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return new PublishedTotals(0, 0);
            }
            return new PublishedTotals(rs.getLong("c"), rs.getLong("r"));
        } catch (SQLException e) {
            log.error("Failed to load published totals", e);
            throw new DataAccessException("Failed to load published totals", e);
        }
    }

    @Override
    public List<CategoryRaised> raisedByCategory() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(RAISED_BY_CATEGORY_SQL);
             ResultSet rs = stmt.executeQuery()) {
            List<CategoryRaised> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new CategoryRaised(
                        rs.getString("label"),
                        rs.getString("emoji"),
                        rs.getString("color_class"),
                        rs.getLong("raised")));
            }
            return out;
        } catch (SQLException e) {
            log.error("Failed to load raised-by-category totals", e);
            throw new DataAccessException("Failed to load raised-by-category totals", e);
        }
    }

    @Override
    public Map<Long, Long> topWeeklyRaised(int limit) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(TOP_WEEKLY_SQL)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Long, Long> out = new HashMap<>();
                while (rs.next()) {
                    out.put(rs.getLong("pid"), rs.getLong("amt"));
                }
                return out;
            }
        } catch (SQLException e) {
            log.error("Failed to load top weekly profiles", e);
            throw new DataAccessException("Failed to load top weekly profiles", e);
        }
    }

    private Profile mapRow(ResultSet rs) throws SQLException {
        Profile p = new Profile();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setBlurb(rs.getString("blurb"));
        p.setEmoji(rs.getString("emoji"));
        p.setGoal(rs.getInt("goal"));
        p.setRaised(rs.getInt("raised"));
        return p;
    }
}
