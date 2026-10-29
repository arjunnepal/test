package com.c3s.ai.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * One-shot message that survives POST -> redirect -> GET, kept in the session rather
 * than on the URL. push() before redirecting, pull() on the next render.
 */
public final class FlashScope {

    private static final String ATTRIBUTE = "flash";

    private FlashScope() {
    }

    public static void push(HttpServletRequest req, String message) {
        req.getSession(true).setAttribute(ATTRIBUTE, message);
    }

    /** Returns the pending message and clears it. null if none. */
    public static String pull(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        String message = (String) session.getAttribute(ATTRIBUTE);
        if (message != null) {
            session.removeAttribute(ATTRIBUTE);
        }
        return message;
    }
}
