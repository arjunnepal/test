package com.c3s.ai.browse.service;

import com.c3s.ai.browse.dao.BrowseProfileDao;
import com.c3s.ai.browse.dao.CategoryDao;
import com.c3s.ai.browse.dao.DonationDao;
import com.c3s.ai.browse.model.Category;
import com.c3s.ai.browse.model.CategoryTotal;
import com.c3s.ai.browse.model.Profile;
import com.c3s.ai.browse.model.ProfileCard;
import com.c3s.ai.browse.model.TickerItem;
import com.c3s.ai.browse.web.BrowsePageData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowseServiceTest {

    private static Category category(long id, String label, String colorClass) {
        Category c = new Category();
        c.setId(id);
        c.setLabel(label);
        c.setColorClass(colorClass);
        return c;
    }

    private static Profile profile(long id, String name, int goal, int raised, Category... cats) {
        Profile p = new Profile();
        p.setId(id);
        p.setName(name);
        p.setBlurb(name + " blurb");
        p.setGoal(goal);
        p.setRaised(raised);
        p.setEmoji("🎓");
        p.setCategories(List.of(cats));
        return p;
    }

    private final Category education = category(1, "Education", "cat-blue");
    private final Category health = category(2, "Health", "cat-red");

    // limit passed to topWeeklyRaised
    private int requestedTopWeekly = -1;

    private BrowseService service(Map<Long, Long> topWeekly) {
        BrowseProfileDao profileDao = new BrowseProfileDao() {
            @Override
            public List<Profile> findPublishedWithCategories() {
                return List.of(
                        profile(10, "Marcus T.", 2000, 1200, education),
                        profile(11, "Dr. Keisha B.", 8000, 8000, health));
            }
            @Override
            public PublishedTotals publishedTotals() {
                return new PublishedTotals(2, 9200);
            }
            @Override
            public List<CategoryRaised> raisedByCategory() {
                return List.of(
                        new CategoryRaised("Education", "🎓", "cat-blue", 1200),
                        new CategoryRaised("Health", "🏥", "cat-red", 8000));
            }
            @Override
            public Map<Long, Long> topWeeklyRaised(int limit) {
                requestedTopWeekly = limit;
                return topWeekly;
            }
        };
        DonationDao donationDao = new DonationDao() {
            @Override
            public List<TickerItem> recentTicker(int limit) {
                return List.of(new TickerItem("Anonymous", 500, "Marcus T."));
            }
            @Override
            public DonorStats donorStats() {
                return new DonorStats(7, 900, 3);
            }
        };
        CategoryDao categoryDao = () -> List.of(education, health);
        return new BrowseService(profileDao, donationDao, categoryDao);
    }

    @Test
    void buildsCardsWithPercentAndStyle() {
        BrowsePageData data = service(Map.of()).buildPage();

        ProfileCard marcus = data.getProfiles().get(0);
        assertEquals("Marcus T.", marcus.name());
        assertEquals(60, marcus.percent());
        assertEquals("te", marcus.tagClass());
        assertEquals("linear-gradient(135deg,var(--uw-blue-light),#b3cef0)", marcus.cardBackground());
        assertEquals(List.of(1L), marcus.categoryIds());
        assertEquals("1", marcus.categoryIdsAttr());

        ProfileCard keisha = data.getProfiles().get(1);
        assertEquals(100, keisha.percent());
        assertEquals("th", keisha.tagClass());
    }

    @Test
    void onlyTheTopWeeklyProfilesGetTheBadge() {
        BrowsePageData data = service(Map.of(10L, 1400L)).buildPage();

        assertEquals(5, requestedTopWeekly);
        ProfileCard marcus = data.getProfiles().get(0);
        assertTrue(marcus.isWeekly());
        assertEquals(1400L, marcus.weeklyRaised());

        ProfileCard keisha = data.getProfiles().get(1);
        assertFalse(keisha.isWeekly());
        assertEquals(0L, keisha.weeklyRaised());
    }

    @Test
    void categoryStripScalesBarsToLargest() {
        BrowsePageData data = service(Map.of()).buildPage();
        List<CategoryTotal> totals = data.getCategoryTotals();

        CategoryTotal edu = totals.stream().filter(t -> t.label().equals("Education")).findFirst().orElseThrow();
        CategoryTotal hea = totals.stream().filter(t -> t.label().equals("Health")).findFirst().orElseThrow();
        assertEquals(100, hea.barPct());
        assertEquals(15, edu.barPct());
        assertEquals("var(--uw-blue)", edu.color());
    }

    @Test
    void statsAndChipsAndTickerAreWiredThrough() {
        BrowsePageData data = service(Map.of()).buildPage();

        assertEquals(2, data.getStats().livesSupported());
        assertEquals(9200, data.getStats().totalRaised());
        assertEquals(7, data.getStats().activeDonors());
        assertEquals(900, data.getStats().raisedThisWeek());
        assertEquals(3, data.getStats().newDonorsThisMonth());

        assertEquals(2, data.getChips().size());
        assertEquals("cat-ed", data.getChips().get(0).chipClass());
        assertEquals(1, data.getTicker().size());
        assertEquals("Anonymous", data.getTicker().get(0).donorLabel());
    }
}
