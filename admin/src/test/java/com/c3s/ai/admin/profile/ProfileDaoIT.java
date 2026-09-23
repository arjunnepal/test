package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The dashboard's profile queries against the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileDaoIT {

    private final JdbcProfileDao dao = new JdbcProfileDao();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    private ProfileListItem byName(String name) {
        return dao.findListItems().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    @Order(1)
    void theListHasEveryProfileWithItsCategoriesInTwoQueries() {
        List<ProfileListItem> list = dao.findListItems();
        assertEquals(10, list.size());
        ProfileListItem marcus = byName("Marcus T.");
        assertEquals(List.of("Education"), marcus.categories().stream().map(CategoryRef::label).toList());
        assertEquals("Jacksonville, FL", marcus.location());
        assertEquals(ProfileStatus.PUBLISHED, marcus.status());
    }

    @Test
    @Order(1)
    void singleColumnLookups() {
        long id = byName("Amara O.").id();
        assertEquals("Amara O.", dao.findName(id).orElseThrow());
        assertEquals(ProfileStatus.PUBLISHED, dao.findStoredState(id).orElseThrow().status());
        assertTrue(dao.findName(999_999).isEmpty());
        assertTrue(dao.findNewestActiveName().isPresent());
        assertEquals(new SidebarStats(10, 21_830), dao.sidebarStats());
    }

    @Test
    @Order(1)
    void theFullProfileRoundTripsThroughAnUpdate() {
        long id = byName("James W.").id();
        Profile p = dao.findById(id).orElseThrow();
        p.setCity("Orange Park");
        p.setCategories(List.of(CategoryRef.idOnly(2), CategoryRef.idOnly(1)));

        assertTrue(dao.updateUnlessArchived(p));

        Profile reloaded = dao.findById(id).orElseThrow();
        assertEquals("Orange Park", reloaded.getCity());
        assertEquals(List.of("Education", "Health"), reloaded.getCategories().stream().map(CategoryRef::label).toList());
    }

    // the database itself refuses to change an archived profile, whatever the caller checked
    @Test
    @Order(2)
    void anArchivedProfileIsNotUpdated() {
        long id = byName("Sophia L.").id();
        Profile p = dao.findById(id).orElseThrow();
        assertTrue(dao.archive(id));

        p.setStatus(ProfileStatus.PUBLISHED);
        p.setName("Republished");
        assertFalse(dao.updateUnlessArchived(p));

        assertEquals(ProfileStatus.ARCHIVED, dao.findStoredState(id).orElseThrow().status());
        assertEquals("Sophia L.", dao.findName(id).orElseThrow());
    }

    @Test
    @Order(3)
    void aNewProfileIsInsertedWithItsCategories() {
        Profile p = new Profile();
        p.setName("It new profile");
        p.setBlurb("blurb");
        p.setGoal(500);
        p.setRaised(0);
        p.setStatus(ProfileStatus.DRAFT);
        p.setCategories(List.of(CategoryRef.idOnly(3)));

        long id = dao.insert(p);

        Profile reloaded = dao.findById(id).orElseThrow();
        assertEquals("It new profile", reloaded.getName());
        assertEquals("Housing", reloaded.getFirstCategory().label());
    }
}
