package com.c3s.ai.core.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** One-shot message that survives POST -> redirect -> GET, kept in the session, not on the URL. */
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
