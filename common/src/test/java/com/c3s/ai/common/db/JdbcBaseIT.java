package com.c3s.ai.common.db;

import com.c3s.ai.common.exception.ConstraintViolationException;
import com.c3s.ai.common.exception.DataAccessException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The shared JDBC base against a real MySQL. Skipped unless DONATION_IT_JDBC_URL is set (see TestDatabase).
class JdbcBaseIT {

    /** Minimal DAO over the seeded categories table. */
    static class CategoryLabels extends JdbcWriter {

        List<String> active() {
            return queryList("Load labels",
                    "SELECT label FROM uwnefl_categories WHERE status = ? ORDER BY label", rs -> rs.getString(1), "active");
        }

        long insert(String label) {
            return insertReturningKey("Insert category",
                    "INSERT INTO uwnefl_categories (label, color_class) VALUES (?, ?)", label, "cat-blue");
        }

        void insertTwoThenFail(String first) {
            inTransaction("Insert two", w -> {
                w.insertReturningKey("INSERT INTO uwnefl_categories (label, color_class) VALUES (?, ?)", first, "cat-blue");
                // same label again -> unique key violation -> whole transaction rolls back
                w.insertReturningKey("INSERT INTO uwnefl_categories (label, color_class) VALUES (?, ?)", first, "cat-blue");
                return null;
            });
        }

        boolean labelExists(String label) {
            return exists("Check label", "SELECT 1 FROM uwnefl_categories WHERE label = ?", label);
        }

        long count() {
            return queryLong("Count", "SELECT COUNT(*) FROM uwnefl_categories");
        }
    }

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void queriesBindParametersAndMapRows() {
        List<String> labels = new CategoryLabels().active();
        assertTrue(labels.contains("Education"));
        assertEquals(labels.stream().sorted().toList(), labels);
    }

    @Test
    void aDuplicateKeyIsAConstraintViolationNamingTheKey() {
        CategoryLabels dao = new CategoryLabels();
        dao.insert("It duplicate label");
        ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
                () -> dao.insert("It duplicate label"));
        assertTrue(e.involves("label"), e.getCause().getMessage());
    }

    @Test
    void aFailedTransactionRollsBackEverything() {
        CategoryLabels dao = new CategoryLabels();
        long before = dao.count();

        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.insertTwoThenFail("It rollback"));

        assertInstanceOf(ConstraintViolationException.class, e);
        assertFalse(dao.labelExists("It rollback"));
        assertEquals(before, dao.count());
    }

    @Test
    void badSqlIsADataAccessExceptionNotAConstraintViolation() {
        CategoryLabels dao = new CategoryLabels() {
            @Override
            long count() {
                return queryLong("Broken", "SELECT COUNT(*) FROM no_such_table");
            }
        };
        DataAccessException e = assertThrows(DataAccessException.class, dao::count);
        assertFalse(e instanceof ConstraintViolationException);
        assertEquals("Broken failed", e.getMessage());
    }
}
