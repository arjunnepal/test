package com.c3s.ai.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** The only place the "authUser" session attribute is read/written. */
public final class AuthSession {

    public static final String ATTRIBUTE = "authUser";

    private AuthSession() {
    }

    /** Signed-in username, or null. */
    public static String currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(ATTRIBUTE);
    }

    public static boolean isAuthenticated(HttpServletRequest req) {
        return currentUser(req) != null;
    }

    /** Start a logged-in session. Drops the old session first (session fixation). */
    public static void begin(HttpServletRequest req, String username) {
        HttpSession existing = req.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        req.getSession(true).setAttribute(ATTRIBUTE, username);
    }

    public static void end(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
