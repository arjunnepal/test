package com.c3s.ai.browse.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Per-session CSRF token for the sign-in and sign-out forms.
public final class Csrf {

    public static final String PARAM = "_csrf";

    private static final String SESSION_ATTR = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {
    }

    // creates the session if needed, so call it before any output
    public static String tokenFor(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(SESSION_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTR, token);
        }
        return token;
    }

    public static boolean matches(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        String expected = (String) session.getAttribute(SESSION_ATTR);
        String presented = req.getParameter(PARAM);
        if (expected == null || presented == null) {
            return false;
        }
        // constant-time compare
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
