package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {

    private static final CategoryRef EDUCATION = new CategoryRef(1, "Education", "E", "cat-blue");
    private static final CategoryRef HEALTH = new CategoryRef(2, "Health", "H", "cat-red");

    /** Stores status only - enough to exercise the rules, with the real DAO's "no update when archived". */
    private static final class FakeDao implements ProfileDao {
        final Map<Long, ProfileStatus> statuses = new HashMap<>();
        Profile lastUpdate;

        public List<ProfileListItem> findListItems() {
            return List.of();
        }

        public Optional<Profile> findById(long id) {
            return Optional.ofNullable(statuses.get(id)).map(s -> {
                Profile p = new Profile();
                p.setId(id);
                p.setStatus(s);
                return p;
            });
        }

        public Optional<StoredState> findStoredState(long id) {
            return Optional.ofNullable(statuses.get(id)).map(s -> new StoredState(s, null));
        }

        public Optional<String> findName(long id) {
            return statuses.containsKey(id) ? Optional.of("Profile " + id) : Optional.empty();
        }

        public Optional<String> findNewestActiveName() {
            return Optional.empty();
        }

        public long insert(Profile profile) {
            return 42;
        }

        public boolean updateUnlessArchived(Profile profile) {
            ProfileStatus current = statuses.get(profile.getId());
            if (current == null || current == ProfileStatus.ARCHIVED) {
                return false;
            }
            lastUpdate = profile;
            return true;
        }

        public boolean archive(long id) {
            return statuses.replace(id, ProfileStatus.ARCHIVED) != null;
        }

        public SidebarStats sidebarStats() {
            return SidebarStats.EMPTY;
        }
    }

    private final FakeDao dao = new FakeDao();
    private final ProfileService service = new ProfileService(dao, () -> List.of(EDUCATION, HEALTH));

    private static Profile valid(ProfileStatus status, long... categoryIds) {
        Profile p = new Profile();
        p.setName("Marcus T.");
        p.setBlurb("Needs books");
        p.setGoal(1000);
        p.setRaised(100);
        p.setStatus(status);
        p.setCategories(java.util.Arrays.stream(categoryIds).mapToObj(CategoryRef::idOnly).toList());
        return p;
    }

    @Test
    void anArchivedProfileCannotBeOpenedOrSavedOver() {
        dao.statuses.put(5L, ProfileStatus.ARCHIVED);
        assertThrows(ArchivedEntityException.class, () -> service.getForEdit(5));
        assertThrows(ArchivedEntityException.class, () -> service.editableState(5));
    }

    // the security review's top finding: a direct POST could re-publish an archived profile
    @Test
    void anArchivedProfileCannotBeRepublishedByADirectUpdate() {
        dao.statuses.put(5L, ProfileStatus.ARCHIVED);
        Profile republish = valid(ProfileStatus.PUBLISHED, 1);
        republish.setId(5L);

        assertThrows(ArchivedEntityException.class, () -> service.update(republish));
        assertEquals(ProfileStatus.ARCHIVED, dao.statuses.get(5L));
        assertNull(dao.lastUpdate);
    }

    @Test
    void updatingAMissingProfileIsNotFound() {
        Profile p = valid(ProfileStatus.DRAFT, 1);
        p.setId(8L);
        assertThrows(EntityNotFoundException.class, () -> service.update(p));
    }

    @Test
    void aPublishedProfileCannotBeSavedAsADraft() {
        Map<String, String> errors = service.validate(valid(ProfileStatus.DRAFT, 1),
                new StoredState(ProfileStatus.PUBLISHED, null));
        assertEquals(ProfileService.UNPUBLISH_REFUSED, errors.get("status"));

        assertTrue(service.validate(valid(ProfileStatus.PUBLISHED, 1), new StoredState(ProfileStatus.PUBLISHED, null)).isEmpty());
        assertTrue(service.validate(valid(ProfileStatus.DRAFT, 1), null).isEmpty());
    }

    @Test
    void onlyActiveCategoriesCanBeAttachedAndTheyAreFilledIn() {
        Profile submitted = valid(ProfileStatus.DRAFT, 2, 1, 2);
        assertTrue(service.validate(submitted, null).isEmpty());
        assertEquals(List.of(HEALTH, EDUCATION), submitted.getCategories(), "full details, duplicates dropped");

        Profile withUnknown = valid(ProfileStatus.DRAFT, 1, 99);
        assertEquals(ProfileService.CATEGORY_UNAVAILABLE, service.validate(withUnknown, null).get("categories"));
    }

    @Test
    void fieldLengthsFollowTheSchema() {
        Profile p = valid(ProfileStatus.DRAFT, 1);
        p.setCity("c".repeat(101));
        p.setEmoji("e".repeat(9));
        p.setBlurb("b".repeat(161));
        Map<String, String> errors = service.validate(p, null);
        assertTrue(errors.containsKey("city"));
        assertTrue(errors.containsKey("emoji"));
        assertTrue(errors.containsKey("blurb"));
        assertFalse(errors.containsKey("name"));
    }

    @Test
    void archiveReturnsTheName() throws Exception {
        dao.statuses.put(3L, ProfileStatus.PUBLISHED);
        assertEquals("Profile 3", service.archive(3));
        assertThrows(EntityNotFoundException.class, () -> service.archive(4));
    }

    @Test
    void listItemsDeriveWhatTheListShows() {
        ProfileListItem item = new ProfileListItem(1, "Rosa", "Jacksonville", "FL", null, " ", 2000, 500,
                ProfileStatus.DRAFT, List.of(HEALTH));
        assertEquals("H", item.displayEmoji());
        assertEquals(25, item.percentRaised());
        assertEquals("Jacksonville, FL", item.location());
        assertFalse(item.hasPhoto());
    }
}
