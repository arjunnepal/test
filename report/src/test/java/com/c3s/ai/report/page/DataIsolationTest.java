package com.c3s.ai.report.page;

import com.c3s.ai.common.db.DBUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The report reads through its own query facades only: its DAOs are hidden, and neither the
 * dashboard's nor the giving site's classes are on its classpath.
 */
class DataIsolationTest {

    @Test
    void theDaosAreHiddenBehindTheirFacades() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.report.donation.ReportDonationDao", "com.c3s.ai.report.donation.JdbcReportDonationDao",
                "com.c3s.ai.report.policy.ReportPolicyDao", "com.c3s.ai.report.policy.JdbcReportPolicyDao",
                "com.c3s.ai.core.user.UserCredentials", "com.c3s.ai.core.user.UserCredentialsDao",
                "com.c3s.ai.core.user.JdbcUserCredentialsDao"}) {
            assertFalse(Modifier.isPublic(Class.forName(name).getModifiers()), name + " must not be public");
        }
    }

    @Test
    void theReportCannotWrite() throws Exception {
        for (String name : new String[]{
                "com.c3s.ai.report.donation.JdbcReportDonationDao", "com.c3s.ai.report.policy.JdbcReportPolicyDao"}) {
            assertFalse(Class.forName("com.c3s.ai.common.db.JdbcWriter").isAssignableFrom(Class.forName(name)),
                    name + " must be read-only");
        }
    }

    @Test
    void thereIsNoPublicWayToOpenARawConnection() throws Exception {
        assertFalse(Modifier.isPublic(DBUtil.class.getDeclaredMethod("getConnection").getModifiers()));
    }

    @Test
    void neitherTheDashboardNorTheGivingSiteIsOnTheClasspath() {
        for (String name : new String[]{
                "com.c3s.ai.admin.allocation.AllocationService", "com.c3s.ai.admin.allocation.JdbcAllocationDao",
                "com.c3s.ai.browse.page.BrowseService", "com.c3s.ai.browse.donation.DonationQueries"}) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(name), name);
        }
    }
}
