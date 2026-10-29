package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.db.DBUtil;
import com.c3s.ai.admin.exception.DataAccessException;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.CategoryStatus;
import com.c3s.ai.admin.model.Profile;
import com.c3s.ai.admin.model.ProfileStatus;
import com.c3s.ai.admin.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcProfileDao implements ProfileDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcProfileDao.class);

    private static final String INSERT_SQL = """
            INSERT INTO uwnefl_profiles
                (name, city, state, blurb, story, emoji, photo_filename, goal, raised,
                 deadline, identity_verified, needs_reviewed, paypal_confirmed,
                 story_reviewed, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE uwnefl_profiles SET
                name = ?, city = ?, state = ?, blurb = ?, story = ?,
                emoji = ?, photo_filename = ?, goal = ?, raised = ?, deadline = ?, identity_verified = ?,
                needs_reviewed = ?, paypal_confirmed = ?, story_reviewed = ?, status = ?
            WHERE id = ?
            """;

    private static final String SELECT_BY_ID_SQL = "SELECT * FROM uwnefl_profiles WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM uwnefl_profiles ORDER BY created_at DESC";
    private static final String SELECT_ALL_ACTIVE_SQL =
            "SELECT * FROM uwnefl_profiles WHERE status <> 'archived' ORDER BY created_at DESC";
    private static final String SELECT_FIRST_SQL = "SELECT * FROM uwnefl_profiles ORDER BY created_at DESC LIMIT 1";
    private static final String SELECT_FIRST_ACTIVE_SQL =
            "SELECT * FROM uwnefl_profiles WHERE status <> 'archived' ORDER BY created_at DESC LIMIT 1";
    private static final String ARCHIVE_SQL = "UPDATE uwnefl_profiles SET status = 'archived' WHERE id = ?";
    private static final String SIDEBAR_STATS_SQL =
            "SELECT COUNT(*) AS published_count, COALESCE(SUM(raised), 0) AS total_raised "
                    + "FROM uwnefl_profiles WHERE status = 'published'";
    private static final String SELECT_CATEGORIES_FOR_PROFILE_SQL = """
            SELECT c.* FROM uwnefl_categories c
            JOIN uwnefl_profile_categories pc ON pc.category_id = c.id
            WHERE pc.profile_id = ?
            ORDER BY c.label ASC
            """;
    private static final String DELETE_PROFILE_CATEGORIES_SQL = "DELETE FROM uwnefl_profile_categories WHERE profile_id = ?";
    private static final String INSERT_PROFILE_CATEGORY_SQL =
            "INSERT INTO uwnefl_profile_categories (profile_id, category_id) VALUES (?, ?)";

    @Override
    public Profile findById(long id) throws EntityNotFoundException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            stmt.setLong(1, id);
            Profile profile;
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntityNotFoundException("profile", id);
                }
                profile = mapRow(rs);
            }
            profile.setCategories(loadCategories(conn, id));
            return profile;
        } catch (SQLException e) {
            log.error("Failed to load profile id={}", id, e);
            throw new DataAccessException("Failed to load profile id=" + id, e);
        }
    }

    @Override
    public List<Profile> findAll(boolean activeOnly) {
        String sql = activeOnly ? SELECT_ALL_ACTIVE_SQL : SELECT_ALL_SQL;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Profile> profiles = new ArrayList<>();
            while (rs.next()) {
                profiles.add(mapRow(rs));
            }
            // TODO N+1 - fine at admin-list sizes, revisit with a join if the list gets big
            for (Profile p : profiles) {
                p.setCategories(loadCategories(conn, p.getId()));
            }
            return profiles;
        } catch (SQLException e) {
            log.error("Failed to load profile list", e);
            throw new DataAccessException("Failed to load profile list", e);
        }
    }

    @Override
    public Optional<Profile> findFirst(boolean activeOnly) {
        String sql = activeOnly ? SELECT_FIRST_ACTIVE_SQL : SELECT_FIRST_SQL;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to load first profile", e);
            throw new DataAccessException("Failed to load first profile", e);
        }
    }

    @Override
    public void archive(long id) throws EntityNotFoundException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ARCHIVE_SQL)) {
            stmt.setLong(1, id);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new EntityNotFoundException("profile", id);
            }
            log.debug("Archived profile id={}", id);
        } catch (SQLException e) {
            log.error("Failed to archive profile id={}", id, e);
            throw new DataAccessException("Failed to archive profile id=" + id, e);
        }
    }

    @Override
    public long insert(Profile profile) {
        try {
            long id = DBUtil.inTransaction(conn -> {
                long newId;
                try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindProfile(stmt, profile);
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Insert did not return a generated id");
                        }
                        newId = keys.getLong(1);
                    }
                }
                insertProfileCategories(conn, newId, profile.getCategories());
                return newId;
            });
            log.debug("Inserted profile id={}", id);
            return id;
        } catch (SQLException e) {
            log.error("Failed to insert profile name={}", Log.clean(profile.getName()), e);
            throw new DataAccessException("Failed to insert profile", e);
        }
    }

    @Override
    public void update(Profile profile) throws EntityNotFoundException {
        boolean rowExisted;
        try {
            rowExisted = DBUtil.inTransaction(conn -> {
                boolean exists;
                try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
                    int i = bindProfile(stmt, profile);
                    stmt.setLong(i, profile.getId());
                    exists = stmt.executeUpdate() > 0;
                }
                if (exists) {
                    try (PreparedStatement del = conn.prepareStatement(DELETE_PROFILE_CATEGORIES_SQL)) {
                        del.setLong(1, profile.getId());
                        del.executeUpdate();
                    }
                    insertProfileCategories(conn, profile.getId(), profile.getCategories());
                }
                return exists;
            });
        } catch (SQLException e) {
            log.error("Failed to update profile id={}", profile.getId(), e);
            throw new DataAccessException("Failed to update profile id=" + profile.getId(), e);
        }
        if (!rowExisted) {
            throw new EntityNotFoundException("profile", profile.getId());
        }
        log.debug("Updated profile id={}", profile.getId());
    }

    @Override
    public SidebarStats getSidebarStats() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SIDEBAR_STATS_SQL);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return new SidebarStats(0, 0);
            }
            return new SidebarStats(rs.getLong("published_count"), rs.getLong("total_raised"));
        } catch (SQLException e) {
            log.error("Failed to load sidebar stats", e);
            throw new DataAccessException("Failed to load sidebar stats", e);
        }
    }

    private void insertProfileCategories(Connection conn, long profileId, List<Category> categories) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_PROFILE_CATEGORY_SQL)) {
            for (Category c : categories) {
                stmt.setLong(1, profileId);
                stmt.setLong(2, c.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private List<Category> loadCategories(Connection conn, long profileId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SELECT_CATEGORIES_FOR_PROFILE_SQL)) {
            stmt.setLong(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getLong("id"));
                    c.setLabel(rs.getString("label"));
                    c.setEmoji(rs.getString("emoji"));
                    c.setColorClass(rs.getString("color_class"));
                    c.setStatus(CategoryStatus.fromDbValue(rs.getString("status")));
                    categories.add(c);
                }
                return categories;
            }
        }
    }

    /** Binds the shared insert/update columns; returns the next param index. */
    private int bindProfile(PreparedStatement stmt, Profile p) throws SQLException {
        int i = 1;
        stmt.setString(i++, p.getName());
        stmt.setString(i++, p.getCity());
        stmt.setString(i++, p.getState());
        stmt.setString(i++, p.getBlurb());
        stmt.setString(i++, p.getStory());
        stmt.setString(i++, p.getOwnEmoji());
        stmt.setString(i++, p.getPhotoFilename());
        stmt.setInt(i++, p.getGoal());
        stmt.setInt(i++, p.getRaised());
        stmt.setDate(i++, p.getDeadline() != null ? Date.valueOf(p.getDeadline()) : null);
        stmt.setBoolean(i++, p.isIdentityVerified());
        stmt.setBoolean(i++, p.isNeedsReviewed());
        stmt.setBoolean(i++, p.isPaypalConfirmed());
        stmt.setBoolean(i++, p.isStoryReviewed());
        stmt.setString(i++, p.getStatus().dbValue());
        return i;
    }

    private Profile mapRow(ResultSet rs) throws SQLException {
        Profile p = new Profile();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setBlurb(rs.getString("blurb"));
        p.setStory(rs.getString("story"));
        p.setEmoji(rs.getString("emoji"));
        p.setPhotoFilename(rs.getString("photo_filename"));
        p.setGoal(rs.getInt("goal"));
        p.setRaised(rs.getInt("raised"));
        Date deadline = rs.getDate("deadline");
        p.setDeadline(deadline != null ? deadline.toLocalDate() : null);
        p.setIdentityVerified(rs.getBoolean("identity_verified"));
        p.setNeedsReviewed(rs.getBoolean("needs_reviewed"));
        p.setPaypalConfirmed(rs.getBoolean("paypal_confirmed"));
        p.setStoryReviewed(rs.getBoolean("story_reviewed"));
        p.setStatus(ProfileStatus.fromDbValue(rs.getString("status")));
        return p;
    }
}
