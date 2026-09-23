package com.c3s.ai.core.auth;

import com.c3s.ai.core.user.AuthenticatedUser;
import com.c3s.ai.core.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * The only place session authentication is read or written. One WAR gets one JSESSIONID, so the
 * admin and user sessions are two independent scopes inside it: signing in on the public site
 * never grants admin access, and both can be live at once as different accounts. A scope ends
 * after its idle timeout, and in any case MAX_AGE after sign-in.
 */
public final class AuthSession {

    /** Which sign-in a request's identity came from, and how long it may sit idle. */
    public enum Scope {

        USER("userAuth", Duration.ofMinutes(30)),
        /** Shorter on purpose, and only refreshed by /admin traffic (see AuthFilter). */
        ADMIN("adminAuth", Duration.ofMinutes(15));

        private final String attribute;
        private final Duration idleTimeout;

        Scope(String attribute, Duration idleTimeout) {
            this.attribute = attribute;
            this.idleTimeout = idleTimeout;
        }

        public String attribute() {
            return attribute;
        }

        public Duration idleTimeout() {
            return idleTimeout;
        }
    }

    /** Hard limit on one sign-in, however active it stays. */
    public static final Duration MAX_AGE = Duration.ofHours(8);

    /** Who is signed in within one scope. Immutable; sliding lastSeen replaces the whole record. */
    public record Principal(String username, String displayName, Role role,
                            Instant establishedAt, Instant lastSeen) implements Serializable {

        boolean isExpired(Scope scope, Instant now) {
            return now.isAfter(lastSeen.plus(scope.idleTimeout())) || now.isAfter(establishedAt.plus(MAX_AGE));
        }

        Principal touched(Instant now) {
            return new Principal(username, displayName, role, establishedAt, now);
        }
    }

    private AuthSession() {
    }

    /** The signed-in principal, or null when absent or expired - an expired scope is cleared here. */
    public static Principal current(HttpServletRequest req, Scope scope) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(scope.attribute());
        if (!(raw instanceof Principal principal)) {
            return null;
        }
        if (principal.isExpired(scope, Instant.now())) {
            end(req, scope);
            return null;
        }
        return principal;
    }

    public static boolean isAuthenticated(HttpServletRequest req, Scope scope) {
        return current(req, scope) != null;
    }

    public static String currentUser(HttpServletRequest req, Scope scope) {
        Principal p = current(req, scope);
        return p == null ? null : p.username();
    }

    public static String currentDisplayName(HttpServletRequest req, Scope scope) {
        Principal p = current(req, scope);
        return p == null ? null : p.displayName();
    }

    public static Role currentRole(HttpServletRequest req, Scope scope) {
        Principal p = current(req, scope);
        return p == null ? null : p.role();
    }

    /** Slides this scope's idle clock; an already-expired scope is ended rather than revived. */
    public static void touch(HttpServletRequest req, Scope scope) {
        Principal principal = current(req, scope);
        if (principal != null) {
            req.getSession(false).setAttribute(scope.attribute(), principal.touched(Instant.now()));
        }
    }

    /**
     * Start a signed-in scope. Rotates the session id rather than invalidating, which would take
     * the other scope down with it, and replaces the CSRF token minted before sign-in.
     */
    public static void begin(HttpServletRequest req, Scope scope, AuthenticatedUser user) {
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        Csrf.rotate(session);
        Instant now = Instant.now();
        session.setAttribute(scope.attribute(),
                new Principal(user.username(), user.displayName(), user.role(), now, now));
    }

    /** End one scope. The session itself only goes away once no scope is left. */
    public static void end(HttpServletRequest req, Scope scope) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(scope.attribute());
        for (Scope other : Scope.values()) {
            if (session.getAttribute(other.attribute()) != null) {
                return;
            }
        }
        // nobody left - drop the session (and with it the CSRF token)
        session.invalidate();
    }
}
