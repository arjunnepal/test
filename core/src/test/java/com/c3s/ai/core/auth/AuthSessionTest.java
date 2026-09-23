package com.c3s.ai.core.auth;

import com.c3s.ai.core.auth.AuthSession.Principal;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.user.AuthenticatedUser;
import com.c3s.ai.core.user.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admin session and the donor session are two scopes in one HttpSession, so the thing
 * worth testing is that they stay independent: opening, expiring or ending one must never
 * disturb the other.
 */
class AuthSessionTest {

    private static AuthenticatedUser user(String username, Role role) {
        return new AuthenticatedUser(username, username + " display", role);
    }

    /** Plant a principal whose last activity was `minutesAgo`, to age a scope without waiting. */
    private static void plantStale(StubSessionRequest req, Scope scope, String username, Role role, int minutesAgo) {
        Instant then = Instant.now().minusSeconds(minutesAgo * 60L);
        req.request.getSession(true).setAttribute(scope.attribute(),
                new Principal(username, username + " display", role, then, then));
    }

    @Test
    void signingInOnOneSideLeavesTheOtherAlone() {
        StubSessionRequest req = new StubSessionRequest();

        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.ADMINISTRATOR));
        AuthSession.begin(req.request, Scope.USER, user("dana", Role.USER));

        assertEquals("boss", AuthSession.currentUser(req.request, Scope.ADMIN));
        assertEquals("dana", AuthSession.currentUser(req.request, Scope.USER));
    }

    // the whole point of the split: a donor session is never an admin session
    @Test
    void theDonorScopeIsNotTheAdminScopeEvenForAnAdministrator() {
        StubSessionRequest req = new StubSessionRequest();

        AuthSession.begin(req.request, Scope.USER, user("boss", Role.ADMINISTRATOR));

        assertTrue(AuthSession.isAuthenticated(req.request, Scope.USER));
        assertFalse(AuthSession.isAuthenticated(req.request, Scope.ADMIN));
        assertNull(AuthSession.currentRole(req.request, Scope.ADMIN));
    }

    // session fixation: begin() must rotate the id, but with changeSessionId() rather than
    // invalidate(), which would take the other scope down with it
    @Test
    void beginRotatesTheSessionIdWithoutDroppingTheOtherScope() {
        StubSessionRequest req = new StubSessionRequest();
        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.ADMINISTRATOR));
        String idAfterAdmin = req.sessionId();

        AuthSession.begin(req.request, Scope.USER, user("dana", Role.USER));

        assertNotEquals(idAfterAdmin, req.sessionId());
        assertEquals("boss", AuthSession.currentUser(req.request, Scope.ADMIN));
    }

    @Test
    void theAdminScopeGoesIdleWhileTheDonorScopeIsStillLive() {
        StubSessionRequest req = new StubSessionRequest();
        // 20 minutes: past the admin scope's 15, inside the donor scope's 30
        plantStale(req, Scope.ADMIN, "boss", Role.ADMINISTRATOR, 20);
        plantStale(req, Scope.USER, "dana", Role.USER, 20);

        assertNull(AuthSession.current(req.request, Scope.ADMIN));
        assertEquals("dana", AuthSession.currentUser(req.request, Scope.USER));
    }

    @Test
    void anExpiredScopeIsClearedNotJustHidden() {
        StubSessionRequest req = new StubSessionRequest();
        plantStale(req, Scope.ADMIN, "boss", Role.ADMINISTRATOR, 20);

        assertNull(AuthSession.current(req.request, Scope.ADMIN));
        assertNull(req.session());   // last scope gone, so the session went with it
    }

    // public-page traffic must not hold the dashboard open
    @Test
    void touchingTheDonorScopeDoesNotSlideTheAdminScope() {
        StubSessionRequest req = new StubSessionRequest();
        plantStale(req, Scope.ADMIN, "boss", Role.ADMINISTRATOR, 14);
        plantStale(req, Scope.USER, "dana", Role.USER, 14);

        AuthSession.touch(req.request, Scope.USER);

        Principal admin = AuthSession.current(req.request, Scope.ADMIN);
        Principal donor = AuthSession.current(req.request, Scope.USER);
        assertTrue(admin.lastSeen().isBefore(donor.lastSeen()),
                "only the donor scope should have moved forward");
    }

    @Test
    void touchKeepsAScopeAlive() {
        StubSessionRequest req = new StubSessionRequest();
        plantStale(req, Scope.ADMIN, "boss", Role.ADMINISTRATOR, 14);

        AuthSession.touch(req.request, Scope.ADMIN);

        assertTrue(AuthSession.current(req.request, Scope.ADMIN).lastSeen()
                .isAfter(Instant.now().minusSeconds(60)));
    }

    @Test
    void signingOutOfOneSideLeavesTheOther() {
        StubSessionRequest req = new StubSessionRequest();
        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.ADMINISTRATOR));
        AuthSession.begin(req.request, Scope.USER, user("dana", Role.USER));

        AuthSession.end(req.request, Scope.USER);

        assertNull(AuthSession.currentUser(req.request, Scope.USER));
        assertEquals("boss", AuthSession.currentUser(req.request, Scope.ADMIN));
        assertFalse(req.sessionInvalidated());
    }

    @Test
    void theSessionGoesAwayOnceTheLastScopeDoes() {
        StubSessionRequest req = new StubSessionRequest();
        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.ADMINISTRATOR));
        AuthSession.begin(req.request, Scope.USER, user("dana", Role.USER));

        AuthSession.end(req.request, Scope.USER);
        AuthSession.end(req.request, Scope.ADMIN);

        assertNull(req.session());
    }

    // an expired scope must not be slid back to life by traffic on its side of the app
    @Test
    void touchEndsAnExpiredScopeInsteadOfRevivingIt() {
        StubSessionRequest req = new StubSessionRequest();
        plantStale(req, Scope.USER, "dana", Role.USER, 45);

        AuthSession.touch(req.request, Scope.USER);

        assertNull(AuthSession.current(req.request, Scope.USER));
        assertNull(req.session());
    }

    @Test
    void aSignInEndsAfterTheMaximumAgeHoweverActiveItIs() {
        StubSessionRequest req = new StubSessionRequest();
        Instant signedIn = Instant.now().minus(AuthSession.MAX_AGE).minusSeconds(60);
        req.request.getSession(true).setAttribute(Scope.USER.attribute(),
                new Principal("dana", "Dana", Role.USER, signedIn, Instant.now()));

        assertNull(AuthSession.current(req.request, Scope.USER));
    }

    // a token read from the pre-sign-in page must be worthless afterwards
    @Test
    void signingInReplacesTheCsrfToken() {
        StubSessionRequest req = new StubSessionRequest();
        String before = Csrf.tokenFor(req.request);

        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.ADMINISTRATOR));

        assertNotEquals(before, Csrf.tokenFor(req.request));
    }

    @Test
    void readingASessionlessRequestIsSafe() {
        StubSessionRequest req = new StubSessionRequest();

        assertNull(AuthSession.current(req.request, Scope.ADMIN));
        assertNull(AuthSession.currentUser(req.request, Scope.USER));
        assertNull(AuthSession.currentDisplayName(req.request, Scope.USER));
        assertFalse(AuthSession.isAuthenticated(req.request, Scope.ADMIN));
        AuthSession.touch(req.request, Scope.USER);   // must not create one
        AuthSession.end(req.request, Scope.USER);
        assertNull(req.session());
    }

    @Test
    void theStoredPrincipalCarriesDisplayNameAndRole() {
        StubSessionRequest req = new StubSessionRequest();
        AuthSession.begin(req.request, Scope.ADMIN, user("boss", Role.EMPLOYEE));

        assertEquals("boss display", AuthSession.currentDisplayName(req.request, Scope.ADMIN));
        assertEquals(Role.EMPLOYEE, AuthSession.currentRole(req.request, Scope.ADMIN));
    }
}
