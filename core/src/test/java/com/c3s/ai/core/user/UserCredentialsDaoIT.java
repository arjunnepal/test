package com.c3s.ai.core.user;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Skipped unless DONATION_IT_JDBC_URL is set (see TestDatabase).
class UserCredentialsDaoIT {

    private final JdbcUserCredentialsDao dao = new JdbcUserCredentialsDao();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void loadsTheRowWithItsRoleAndOptionalPassword() {
        UserCredentials admin = dao.findByUsername("admin").orElseThrow();
        assertEquals(Role.ADMINISTRATOR, admin.role());
        assertEquals("Administrator", admin.displayName());
        assertNotNull(admin.passwordHash());

        assertNull(dao.findByUsername("cwashington").orElseThrow().passwordHash());
        assertTrue(dao.findByUsername("nobody").isEmpty());
    }

    // The login throttle keys on the row this returns, which only closes the accent/case bypass
    // if MySQL really does match these spellings to the one account.
    @Test
    void caseAndAccentVariantsFindTheSameAccount() {
        for (String variant : new String[]{"ADMIN", "ádmin", "Admín"}) {
            assertEquals("admin", dao.findByUsername(variant).orElseThrow().username(), variant);
        }
    }
}
