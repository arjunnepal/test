package com.c3s.ai.browse.page;

import com.c3s.ai.common.db.DBUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The giving site must not be able to reach dashboard data or credentials. These checks run
 * against the real test classpath, so a dependency change that breaks the boundary fails here.
 */
class DataIsolationTest {

    @Test
    void noDashboardClassIsOnTheGivingSitesClasspath() {
        for (String name : new String[]{
                "com.c3s.ai.admin.profile.ProfileService",
                "com.c3s.ai.admin.profile.JdbcProfileDao",
                "com.c3s.ai.admin.category.JdbcCategoryDao",
                "com.c3s.ai.admin.allocation.JdbcAllocationDao"}) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(name), name);
        }
    }

    @Test
    void passwordHashesStayInsideTheUserPackage() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.core.user.UserCredentials",
                "com.c3s.ai.core.user.UserCredentialsDao",
                "com.c3s.ai.core.user.JdbcUserCredentialsDao"}) {
            assertFalse(Modifier.isPublic(Class.forName(name).getModifiers()), name + " must not be public");
        }
    }

    @Test
    void thereIsNoPublicWayToOpenARawConnection() throws Exception {
        assertFalse(Modifier.isPublic(DBUtil.class.getDeclaredMethod("getConnection").getModifiers()));
    }

    // each table folder lets out one public query facade; the DAO behind it stays inside
    @Test
    void theSiteDaosAreHiddenBehindTheirQueryFacades() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.browse.profile.SiteProfileDao", "com.c3s.ai.browse.profile.JdbcSiteProfileDao",
                "com.c3s.ai.browse.category.SiteCategoryDao", "com.c3s.ai.browse.category.JdbcSiteCategoryDao",
                "com.c3s.ai.browse.donation.SiteDonationDao", "com.c3s.ai.browse.donation.JdbcSiteDonationDao"}) {
            assertFalse(Modifier.isPublic(Class.forName(name).getModifiers()), name + " must not be public");
        }
    }

    @Test
    void theGivingSiteHasNoWayToWrite() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.browse.profile.JdbcSiteProfileDao",
                "com.c3s.ai.browse.category.JdbcSiteCategoryDao",
                "com.c3s.ai.browse.donation.JdbcSiteDonationDao"}) {
            Class<?> dao = Class.forName(name);
            assertFalse(com.c3s.ai.common.db.JdbcWriter.class.isAssignableFrom(dao), name + " must not be able to write");
        }
    }
}
