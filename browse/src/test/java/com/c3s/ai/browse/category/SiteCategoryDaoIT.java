package com.c3s.ai.browse.category;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The chip strip's query against the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
class SiteCategoryDaoIT {

    private final CategoryQueries categories = CategoryQueries.create();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void chipsListOnlyCategoriesInUseInCreationOrder() {
        assertEquals(List.of("Education", "Health", "Housing", "Food & Nutrition", "Veterans"),
                categories.findInUse().stream().map(ChipCategory::label).toList());
    }

    // an empty category has no chip, and an archived one loses its chip even while it's still in use
    @Test
    void anArchivedOrUnusedCategoryHasNoChip() throws Exception {
        TestDatabase.exec("INSERT INTO uwnefl_categories (label, emoji, color_class) VALUES ('Chip test', NULL, 'cat-purple')");
        try {
            assertTrue(chips().stream().noneMatch(l -> l.equals("Chip test")), "unused category has no chip");

            TestDatabase.exec("""
                    INSERT INTO uwnefl_profile_categories (profile_id, category_id)
                    SELECT p.id, c.id FROM uwnefl_profiles p, uwnefl_categories c
                    WHERE p.status = 'published' AND c.label = 'Chip test'
                    ORDER BY p.id LIMIT 1""");
            assertTrue(chips().contains("Chip test"), "a category in use gets a chip");

            TestDatabase.exec("UPDATE uwnefl_categories SET status = 'archived' WHERE label = 'Chip test'");
            assertTrue(chips().stream().noneMatch(l -> l.equals("Chip test")), "an archived category loses its chip");
        } finally {
            // leave the seeded data as we found it - the other IT classes assert exact lists
            TestDatabase.exec("""
                    DELETE pc FROM uwnefl_profile_categories pc
                    JOIN uwnefl_categories c ON c.id = pc.category_id
                    WHERE c.label = 'Chip test'""");
            TestDatabase.exec("DELETE FROM uwnefl_categories WHERE label = 'Chip test'");
        }
    }

    private List<String> chips() {
        return categories.findInUse().stream().map(ChipCategory::label).toList();
    }
}
