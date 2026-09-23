package com.c3s.ai.admin.category;

import com.c3s.ai.common.db.TestDatabase;
import com.c3s.ai.common.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Skipped unless DONATION_IT_JDBC_URL is set (see TestDatabase).
class CategoryDaoIT {

    private final JdbcCategoryDao dao = new JdbcCategoryDao();
    private final CategoryService service = new CategoryService(dao);

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    private static Category named(String label) {
        Category c = new Category();
        c.setLabel(label);
        c.setColorClass("cat-green");
        return c;
    }

    @Test
    void anArchivedCategoryIsNotUpdatedByTheDatabase() {
        Category c = named("It archive me");
        assertTrue(service.create(c).isEmpty());
        assertTrue(dao.archive(c.getId()));

        c.setLabel("It renamed");
        assertFalse(dao.updateActive(c));
        assertEquals("It archive me", dao.findLabel(c.getId()).orElseThrow());
    }

    @Test
    void aDuplicateLabelIsReportedAsAFieldError() {
        assertTrue(service.create(named("It unique")).isEmpty());
        assertEquals(CategoryService.DUPLICATE_LABEL, service.create(named("It unique")).get("label"));
        assertThrows(ConstraintViolationException.class, () -> dao.insert(named("It unique")));
    }

    @Test
    void activeRefsAreByLabelWithoutArchived() {
        var labels = dao.findActiveRefs().stream().map(CategoryRef::label).toList();
        assertEquals(labels.stream().sorted().toList(), labels);
        assertFalse(labels.contains("It archive me"));
    }
}
