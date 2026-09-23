package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.db.JdbcWriter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcProfileDao extends JdbcWriter implements ProfileDao {

    private static final String SELECT_LIST_SQL = """
            SELECT id, name, city, state, emoji, photo_filename, goal, raised, status
            FROM uwnefl_profiles
            ORDER BY created_at DESC, id DESC
            """;

    // one query for every profile's categories, instead of one per profile
    private static final String SELECT_LIST_CATEGORIES_SQL = """
            SELECT pc.profile_id, c.id, c.label, c.emoji, c.color_class
            FROM uwnefl_profile_categories pc
            JOIN uwnefl_categories c ON c.id = pc.category_id
            ORDER BY c.label ASC
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, name, city, state, blurb, story, emoji, photo_filename, goal, raised, deadline,
                   identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status
            FROM uwnefl_profiles
            WHERE id = ?
            """;

    private static final String SELECT_CATEGORIES_FOR_PROFILE_SQL = """
            SELECT c.id, c.label, c.emoji, c.color_class
            FROM uwnefl_profile_categories pc
            JOIN uwnefl_categories c ON c.id = pc.category_id
            WHERE pc.profile_id = ?
            ORDER BY c.label ASC
            """;

    private static final String SELECT_STORED_STATE_SQL = "SELECT status, photo_filename FROM uwnefl_profiles WHERE id = ?";
    private static final String SELECT_NAME_SQL = "SELECT name FROM uwnefl_profiles WHERE id = ?";
    private static final String SELECT_NEWEST_ACTIVE_NAME_SQL =
            "SELECT name FROM uwnefl_profiles WHERE status <> 'archived' ORDER BY created_at DESC, id DESC LIMIT 1";

    private static final String INSERT_SQL = """
            INSERT INTO uwnefl_profiles
                (name, city, state, blurb, story, emoji, photo_filename, goal, raised,
                 deadline, identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // the status condition makes an archived profile impossible to edit, however the request got here
    private static final String UPDATE_UNLESS_ARCHIVED_SQL = """
            UPDATE uwnefl_profiles SET
                name = ?, city = ?, state = ?, blurb = ?, story = ?, emoji = ?, photo_filename = ?,
                goal = ?, raised = ?, deadline = ?, identity_verified = ?, needs_reviewed = ?,
                paypal_confirmed = ?, story_reviewed = ?, status = ?
            WHERE id = ? AND status <> 'archived'
            """;

    private static final String ARCHIVE_SQL = "UPDATE uwnefl_profiles SET status = 'archived' WHERE id = ?";
    private static final String DELETE_CATEGORIES_SQL = "DELETE FROM uwnefl_profile_categories WHERE profile_id = ?";
    private static final String INSERT_CATEGORY_SQL =
            "INSERT INTO uwnefl_profile_categories (profile_id, category_id) VALUES (?, ?)";
    private static final String SIDEBAR_STATS_SQL =
            "SELECT COUNT(*), COALESCE(SUM(raised), 0) FROM uwnefl_profiles WHERE status = 'published'";

    private record ListRow(long id, String name, String city, String state, String emoji, String photoFilename,
                           int goal, int raised, ProfileStatus status) {
    }

    @Override
    public List<ProfileListItem> findListItems() {
        return read("Load profile list", r -> {
            Map<Long, ListRow> rows = new LinkedHashMap<>();
            for (ListRow row : r.list(SELECT_LIST_SQL, rs -> new ListRow(rs.getLong("id"), rs.getString("name"),
                    rs.getString("city"), rs.getString("state"), rs.getString("emoji"), rs.getString("photo_filename"),
                    rs.getInt("goal"), rs.getInt("raised"), ProfileStatus.fromDbValue(rs.getString("status"))))) {
                rows.put(row.id(), row);
            }
            Map<Long, List<CategoryRef>> categories = new HashMap<>();
            r.list(SELECT_LIST_CATEGORIES_SQL, rs -> {
                categories.computeIfAbsent(rs.getLong("profile_id"), k -> new ArrayList<>()).add(new CategoryRef(
                        rs.getLong("id"), rs.getString("label"), rs.getString("emoji"), rs.getString("color_class")));
                return null;
            });
            List<ProfileListItem> out = new ArrayList<>(rows.size());
            for (ListRow row : rows.values()) {
                out.add(new ProfileListItem(row.id(), row.name(), row.city(), row.state(), row.emoji(),
                        row.photoFilename(), row.goal(), row.raised(), row.status(),
                        categories.getOrDefault(row.id(), List.of())));
            }
            return out;
        });
    }

    @Override
    public Optional<Profile> findById(long id) {
        return read("Load profile", r -> {
            Optional<Profile> found = r.optional(SELECT_BY_ID_SQL, rs -> {
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
                p.setDeadline(rs.getObject("deadline", LocalDate.class));
                p.setIdentityVerified(rs.getBoolean("identity_verified"));
                p.setNeedsReviewed(rs.getBoolean("needs_reviewed"));
                p.setPaypalConfirmed(rs.getBoolean("paypal_confirmed"));
                p.setStoryReviewed(rs.getBoolean("story_reviewed"));
                p.setStatus(ProfileStatus.fromDbValue(rs.getString("status")));
                return p;
            }, id);
            if (found.isPresent()) {
                found.get().setCategories(r.list(SELECT_CATEGORIES_FOR_PROFILE_SQL, rs -> new CategoryRef(
                        rs.getLong("id"), rs.getString("label"), rs.getString("emoji"), rs.getString("color_class")), id));
            }
            return found;
        });
    }

    @Override
    public Optional<StoredState> findStoredState(long id) {
        return queryOptional("Load profile state", SELECT_STORED_STATE_SQL, rs -> new StoredState(
                ProfileStatus.fromDbValue(rs.getString("status")), rs.getString("photo_filename")), id);
    }

    @Override
    public Optional<String> findName(long id) {
        return queryOptional("Load profile name", SELECT_NAME_SQL, rs -> rs.getString(1), id);
    }

    @Override
    public Optional<String> findNewestActiveName() {
        return queryOptional("Load newest profile name", SELECT_NEWEST_ACTIVE_NAME_SQL, rs -> rs.getString(1));
    }

    @Override
    public long insert(Profile p) {
        return inTransaction("Insert profile", w -> {
            long id = w.insertReturningKey(INSERT_SQL, columns(p));
            w.batch(INSERT_CATEGORY_SQL, p.getCategories(), c -> new Object[]{id, c.id()});
            return id;
        });
    }

    @Override
    public boolean updateUnlessArchived(Profile p) {
        return inTransaction("Update profile", w -> {
            Object[] columns = columns(p);
            Object[] params = new Object[columns.length + 1];
            System.arraycopy(columns, 0, params, 0, columns.length);
            params[columns.length] = p.getId();
            if (w.update(UPDATE_UNLESS_ARCHIVED_SQL, params) == 0) {
                return false;
            }
            w.update(DELETE_CATEGORIES_SQL, p.getId());
            w.batch(INSERT_CATEGORY_SQL, p.getCategories(), c -> new Object[]{p.getId(), c.id()});
            return true;
        });
    }

    @Override
    public boolean archive(long id) {
        return update("Archive profile", ARCHIVE_SQL, id) > 0;
    }

    @Override
    public SidebarStats sidebarStats() {
        return queryOptional("Load sidebar stats", SIDEBAR_STATS_SQL, rs -> new SidebarStats(rs.getLong(1), rs.getLong(2)))
                .orElse(SidebarStats.EMPTY);
    }

    private static Object[] columns(Profile p) {
        return new Object[]{
                p.getName(), p.getCity(), p.getState(), p.getBlurb(), p.getStory(), p.getOwnEmoji(),
                p.getPhotoFilename(), p.getGoal(), p.getRaised(), p.getDeadline(), p.isIdentityVerified(),
                p.isNeedsReviewed(), p.isPaypalConfirmed(), p.isStoryReviewed(), p.getStatus()};
    }
}
