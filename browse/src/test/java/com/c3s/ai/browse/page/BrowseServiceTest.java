package com.c3s.ai.browse.page;

import com.c3s.ai.browse.category.CategoryQueries;
import com.c3s.ai.browse.category.ChipCategory;
import com.c3s.ai.browse.donation.DonationQueries;
import com.c3s.ai.browse.donation.DonorStats;
import com.c3s.ai.browse.donation.TickerItem;
import com.c3s.ai.browse.profile.CardCategory;
import com.c3s.ai.browse.profile.CategoryRaised;
import com.c3s.ai.browse.profile.PublishedProfile;
import com.c3s.ai.browse.profile.ProfileQueries;
import com.c3s.ai.browse.profile.PublishedTotals;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowseServiceTest {

    private static final String GRAD_CAP = new String(Character.toChars(0x1F393));
    private static final String HOSPITAL = new String(Character.toChars(0x1F3E5));

    private final CardCategory education = new CardCategory(1, "Education", GRAD_CAP, "cat-blue");
    private final CardCategory health = new CardCategory(2, "Health", HOSPITAL, "cat-red");

    // limit passed to topWeeklyRaised
    private int requestedTopWeekly = -1;

    private BrowseService service(Map<Long, Long> topWeekly, List<PublishedProfile> published) {
        ProfileQueries profiles = new ProfileQueries() {
            public List<PublishedProfile> findPublished() {
                return published;
            }
            public PublishedTotals publishedTotals() {
                return new PublishedTotals(2, 9200);
            }
            public List<CategoryRaised> raisedByCategory() {
                return List.of(
                        new CategoryRaised("Education", GRAD_CAP, "cat-blue", 1200),
                        new CategoryRaised("Health", HOSPITAL, "cat-red", 8000));
            }
            public Map<Long, Long> topWeeklyRaised(int limit) {
                requestedTopWeekly = limit;
                return topWeekly;
            }
        };
        DonationQueries donations = new DonationQueries() {
            public List<TickerItem> recentTicker(int limit) {
                return List.of(new TickerItem("Anonymous", 500, "Marcus T."));
            }
            public DonorStats donorStats() {
                return new DonorStats(7, 900, 3);
            }
        };
        CategoryQueries categories = () -> List.of(
                new ChipCategory(1, "Education", GRAD_CAP, "cat-blue"),
                new ChipCategory(2, "Health", HOSPITAL, "cat-red"));
        return new BrowseService(profiles, donations, categories);
    }

    private BrowseService service(Map<Long, Long> topWeekly) {
        return service(topWeekly, List.of(
                new PublishedProfile(10, "Marcus T.", "Marcus blurb", null, 2000, 1200, List.of(education)),
                new PublishedProfile(11, "Dr. Keisha B.", "Keisha blurb", "K", 8000, 8000, List.of(health))));
    }

    @Test
    void buildsCardsWithPercentStyleAndEmojiFallback() {
        BrowsePageData data = service(Map.of()).buildPage();

        ProfileCard marcus = data.getProfiles().get(0);
        assertEquals("Marcus T.", marcus.name());
        assertEquals(60, marcus.percent());
        assertEquals(GRAD_CAP, marcus.emoji(), "no emoji of its own, so the category's");
        assertEquals("te", marcus.tagClass());
        assertEquals("linear-gradient(135deg,var(--uw-blue-light),#b3cef0)", marcus.cardBackground());
        assertEquals(List.of(1L), marcus.categoryIds());
        assertEquals("1", marcus.categoryIdsAttr());

        ProfileCard keisha = data.getProfiles().get(1);
        assertEquals(100, keisha.percent());
        assertEquals("K", keisha.emoji());
        assertEquals("th", keisha.tagClass());
    }

    @Test
    void aProfileWithNoActiveCategoriesStillRenders() {
        BrowsePageData data = service(Map.of(), List.of(
                new PublishedProfile(12, "Rosa D.", "blurb", null, 1000, 0, List.of()))).buildPage();

        ProfileCard rosa = data.getProfiles().get(0);
        assertEquals("", rosa.tagLabel());
        assertEquals("te", rosa.tagClass());
        assertEquals("", rosa.categoryIdsAttr());
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

    // one instance is shared by every request through the page cache
    @Test
    void thePageDataCannotBeChangedOnceBuilt() {
        BrowsePageData data = service(Map.of()).buildPage();

        assertThrows(UnsupportedOperationException.class, () -> data.getProfiles().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.getTicker().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.setMessage("hi"));
    }
}
