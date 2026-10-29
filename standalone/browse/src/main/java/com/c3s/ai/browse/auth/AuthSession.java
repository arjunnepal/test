package com.c3s.ai.browse.auth;

import com.c3s.ai.browse.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class AuthSession {

    private static final String USERNAME = "authUser";
    private static final String DISPLAY_NAME = "authDisplayName";

    private AuthSession() {
    }

    public static String currentUser(HttpServletRequest req) {
        return attribute(req, USERNAME);
    }

    public static String currentDisplayName(HttpServletRequest req) {
        return attribute(req, DISPLAY_NAME);
    }

    public static boolean isAuthenticated(HttpServletRequest req) {
        return currentUser(req) != null;
    }

    // new session on login to prevent session fixation
    public static void begin(HttpServletRequest req, User user) {
        HttpSession existing = req.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(USERNAME, user.getUsername());
        session.setAttribute(DISPLAY_NAME, user.getDisplayName());
    }

    public static void end(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static String attribute(HttpServletRequest req, String name) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(name);
    }
}
