package com.c3s.ai.admin.shell;

import com.c3s.ai.common.db.DBUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Dashboard DAOs are only reachable through their services, and credentials not at all. */
class DataIsolationTest {

    @Test
    void theDaosAreHiddenBehindTheirServices() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.admin.profile.ProfileDao", "com.c3s.ai.admin.profile.JdbcProfileDao",
                "com.c3s.ai.admin.category.CategoryDao", "com.c3s.ai.admin.category.JdbcCategoryDao",
                "com.c3s.ai.admin.allocation.AllocationDao", "com.c3s.ai.admin.allocation.JdbcAllocationDao",
                "com.c3s.ai.core.user.UserCredentials", "com.c3s.ai.core.user.UserCredentialsDao",
                "com.c3s.ai.core.user.JdbcUserCredentialsDao"}) {
            assertFalse(Modifier.isPublic(Class.forName(name).getModifiers()), name + " must not be public");
        }
    }

    @Test
    void thereIsNoPublicWayToOpenARawConnection() throws Exception {
        assertFalse(Modifier.isPublic(DBUtil.class.getDeclaredMethod("getConnection").getModifiers()));
    }

    @Test
    void theGivingSitesClassesAreNotOnTheDashboardsClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.c3s.ai.browse.page.BrowseService"));
    }
}
