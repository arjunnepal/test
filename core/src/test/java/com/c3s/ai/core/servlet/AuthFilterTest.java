package com.c3s.ai.core.servlet;

import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.user.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The route table: which scope and which role opens which door. */
class AuthFilterTest {

    // ---- the public site needs nothing ----

    @Test
    void theGivingPageIsPublic() throws Exception {
        FilterHarness r = FilterHarness.get("/");
        r.run();
        assertTrue(r.passedThrough);
    }

    @Test
    void staticAssetsAndHealthArePublic() throws Exception {
        for (String path : new String[]{"/css/app.css", "/js/core.js", "/healthz"}) {
            FilterHarness r = FilterHarness.get(path);
            r.run();
            assertTrue(r.passedThrough, path + " should be public");
        }
    }

    @Test
    void bothSignInPagesArePublic() throws Exception {
        for (String path : new String[]{"/login", "/admin/login"}) {
            FilterHarness r = FilterHarness.get(path);
            r.run();
            assertTrue(r.passedThrough, path + " should be reachable signed out");
        }
    }

    // ---- /admin needs the admin scope, and only the admin scope ----

    @Test
    void adminPagesRedirectWhenThereIsNoAdminSession() throws Exception {
        FilterHarness r = FilterHarness.get("/admin/profiles");
        r.run();
        assertFalse(r.passedThrough);
        assertEquals("/admin/login", r.redirectedTo);
    }

    // the heart of the two-scope model
    @Test
    void aDonorSessionIsNoHelpOnTheAdminSideEvenForAnAdministrator() throws Exception {
        FilterHarness r = FilterHarness.get("/admin/profiles")
                .signedIn(Scope.USER, "boss", Role.ADMINISTRATOR);
        r.run();
        assertFalse(r.passedThrough);
        assertEquals("/admin/login", r.redirectedTo);
        assertNull(r.errorStatus, "no admin session at all is a redirect, not a 403");
    }

    @Test
    void anEmployeeReachesTheProfilePages() throws Exception {
        for (String path : new String[]{"/admin", "/admin/profiles", "/admin/profile", "/admin/profiles/archive"}) {
            FilterHarness r = FilterHarness.get(path).signedIn(Scope.ADMIN, "casework", Role.EMPLOYEE);
            r.run();
            assertTrue(r.passedThrough, "an employee should reach " + path);
        }
    }

    @Test
    void anEmployeeIsRefusedTheConfigurationPages() throws Exception {
        for (String path : new String[]{"/admin/categories", "/admin/categories/archive",
                "/admin/category", "/admin/allocations", "/admin/allocation"}) {
            FilterHarness r = FilterHarness.get(path).signedIn(Scope.ADMIN, "casework", Role.EMPLOYEE);
            r.run();
            assertFalse(r.passedThrough, "an employee should not reach " + path);
            assertEquals(403, r.errorStatus, path + " should be 403, not a redirect");
        }
    }

    // the role rules are an allow-list: an admin page nobody listed is ADMINISTRATOR-only
    @Test
    void anUnlistedAdminPageIsClosedToEmployees() throws Exception {
        FilterHarness employee = FilterHarness.get("/admin/some-new-report").signedIn(Scope.ADMIN, "casework", Role.EMPLOYEE);
        employee.run();
        assertFalse(employee.passedThrough);
        assertEquals(403, employee.errorStatus);

        FilterHarness administrator = FilterHarness.get("/admin/some-new-report").signedIn(Scope.ADMIN, "boss", Role.ADMINISTRATOR);
        administrator.run();
        assertTrue(administrator.passedThrough);
    }

    @Test
    void adminResponsesAreNeverCached() throws Exception {
        FilterHarness page = FilterHarness.get("/admin/profiles").signedIn(Scope.ADMIN, "boss", Role.ADMINISTRATOR);
        page.run();
        assertEquals("no-store", page.headers.get("Cache-Control"));

        FilterHarness login = FilterHarness.get("/admin/login");
        login.run();
        assertEquals("no-store", login.headers.get("Cache-Control"));

        FilterHarness publicPage = FilterHarness.get("/");
        publicPage.run();
        assertNull(publicPage.headers.get("Cache-Control"));
    }

    @Test
    void anAdministratorReachesEverything() throws Exception {
        for (String path : new String[]{"/admin/profiles", "/admin/categories", "/admin/allocations"}) {
            FilterHarness r = FilterHarness.get(path).signedIn(Scope.ADMIN, "boss", Role.ADMINISTRATOR);
            r.run();
            assertTrue(r.passedThrough, "an administrator should reach " + path);
        }
    }

    // a USER row that somehow holds an admin scope still gets nothing
    @Test
    void theUserRoleIsRefusedEvenWithAnAdminScope() throws Exception {
        FilterHarness r = FilterHarness.get("/admin/profiles").signedIn(Scope.ADMIN, "dana", Role.USER);
        r.run();
        assertFalse(r.passedThrough);
        assertEquals(403, r.errorStatus);
    }

    @Test
    void uploadedPhotosAreStaffOnly() throws Exception {
        FilterHarness anonymous = FilterHarness.get("/photos/abc.jpg");
        anonymous.run();
        assertFalse(anonymous.passedThrough);
        assertEquals("/admin/login", anonymous.redirectedTo);

        FilterHarness staff = FilterHarness.get("/photos/abc.jpg").signedIn(Scope.ADMIN, "casework", Role.EMPLOYEE);
        staff.run();
        assertTrue(staff.passedThrough);
    }

    // /administrators must not be mistaken for something under /admin
    @Test
    void onlyRealAdminPathsAreGated() throws Exception {
        FilterHarness r = FilterHarness.get("/administrators");
        r.run();
        assertTrue(r.passedThrough);
    }

    // ---- CSRF ----

    @Test
    void aWriteWithoutACsrfTokenIsRefusedOnBothSides() throws Exception {
        FilterHarness donor = FilterHarness.post("/logout").signedIn(Scope.USER, "dana", Role.USER);
        donor.run();
        assertFalse(donor.passedThrough);
        assertEquals("/", donor.redirectedTo);

        FilterHarness staff = FilterHarness.post("/admin/category").signedIn(Scope.ADMIN, "boss", Role.ADMINISTRATOR);
        staff.run();
        assertFalse(staff.passedThrough);
        assertEquals("/admin/profiles", staff.redirectedTo);
        assertEquals(AuthFilter.FORM_EXPIRED_MESSAGE, staff.sessionAttribute("flash"));
    }

    // otherwise any same-site page could sign an admin out with one tokenless POST
    @Test
    void aBadCsrfTokenDoesNotEndTheSession() throws Exception {
        FilterHarness staff = FilterHarness.post("/admin/profiles/archive").signedIn(Scope.ADMIN, "boss", Role.ADMINISTRATOR);
        staff.run();
        assertFalse(staff.passedThrough);
        assertNotNull(staff.sessionAttribute(Scope.ADMIN.attribute()));
    }

    @Test
    void aStaleSignInFormIsSimplyShownAgain() throws Exception {
        FilterHarness r = FilterHarness.post("/admin/login");
        r.run();
        assertFalse(r.passedThrough);
        assertEquals("/admin/login", r.redirectedTo);
    }

    // ProfileServlet checks this one itself, after parsing the multipart body
    @Test
    void theMultipartProfilePostSkipsTheFilterCsrfCheck() throws Exception {
        FilterHarness r = FilterHarness.post("/admin/profile").signedIn(Scope.ADMIN, "casework", Role.EMPLOYEE);
        r.run();
        assertTrue(r.passedThrough);
    }
}
