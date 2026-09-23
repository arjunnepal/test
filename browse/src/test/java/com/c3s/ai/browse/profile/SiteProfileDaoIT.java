package com.c3s.ai.browse.profile;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The giving page's profile queries against the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SiteProfileDaoIT {

    private final ProfileQueries profiles = ProfileQueries.create();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    @Order(1)
    void publishedProfilesComeBackInFeaturedOrderWithTheirCategory() {
        List<PublishedProfile> published = profiles.findPublished();

        assertEquals(10, published.size());
        assertEquals("Marcus T.", published.get(0).name());
        assertEquals("Sgt. Patricia H.", published.get(9).name());
        assertEquals("Education", published.get(0).firstCategory().label());
        assertEquals("Food & Nutrition", published.get(3).firstCategory().label());
    }

    @Test
    @Order(1)
    void totalsAndCategoryStripAreComputedFromPublishedProfiles() {
        assertEquals(new PublishedTotals(10, 21_830), profiles.publishedTotals());
        List<CategoryRaised> strip = profiles.raisedByCategory();
        assertEquals(List.of("Education", "Health", "Housing", "Food & Nutrition", "Veterans"),
                strip.stream().map(CategoryRaised::label).toList());
        assertEquals(List.of(1_700L, 9_300L, 3_000L, 1_130L, 6_700L),
                strip.stream().map(CategoryRaised::raised).toList());
    }

    @Test
    @Order(1)
    void topWeeklyIsTheFiveBiggestSevenDayTotals() {
        Map<Long, Long> top = profiles.topWeeklyRaised(5);

        Map<String, Long> byName = new HashMap<>();
        for (PublishedProfile p : profiles.findPublished()) {
            if (top.containsKey(p.id())) {
                byName.put(p.name(), top.get(p.id()));
            }
        }
        assertEquals(Map.of("Sgt. Patricia H.", 2_500L, "Diane & Ray K.", 1_800L, "Marcus T.", 1_400L,
                "Amara O.", 900L, "James W.", 750L), byName);
    }

    // Runs last: it changes data. A profile in two categories lists them by label, and an
    // archived category drops off the cards just as it drops off the chips.
    @Test
    @Order(2)
    void cardsShowActiveCategoriesOnlyInLabelOrder() throws Exception {
        TestDatabase.exec("INSERT INTO uwnefl_categories (label, emoji, color_class) VALUES ('Arts', NULL, 'cat-purple')");
        try {
            TestDatabase.exec("""
                    INSERT INTO uwnefl_profile_categories (profile_id, category_id)
                    SELECT p.id, c.id FROM uwnefl_profiles p, uwnefl_categories c
                    WHERE p.name = 'Marcus T.' AND c.label IN ('Arts', 'Veterans')""");

            PublishedProfile marcus = profiles.findPublished().get(0);
            assertEquals(List.of("Arts", "Education", "Veterans"),
                    marcus.categories().stream().map(CardCategory::label).toList());

            TestDatabase.exec("UPDATE uwnefl_categories SET status = 'archived' WHERE label = 'Arts'");
            marcus = profiles.findPublished().get(0);
            assertEquals(List.of("Education", "Veterans"),
                    marcus.categories().stream().map(CardCategory::label).toList());
        } finally {
            // leave the seeded data as we found it - the other IT classes assert exact lists
            TestDatabase.exec("""
                    DELETE pc FROM uwnefl_profile_categories pc
                    JOIN uwnefl_profiles p ON p.id = pc.profile_id
                    JOIN uwnefl_categories c ON c.id = pc.category_id
                    WHERE p.name = 'Marcus T.' AND c.label IN ('Arts', 'Veterans')""");
            TestDatabase.exec("DELETE FROM uwnefl_categories WHERE label = 'Arts'");
        }
    }
}
