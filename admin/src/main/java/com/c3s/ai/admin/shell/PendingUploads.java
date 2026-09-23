package com.c3s.ai.admin.shell;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashSet;
import java.util.Set;

/**
 * Photos uploaded in this session that no profile owns yet. A rejected form carries its upload's
 * filename in a hidden field, and that name is honoured only if this session uploaded it.
 */
public final class PendingUploads {

    private static final String ATTRIBUTE = PendingUploads.class.getName();

    private PendingUploads() {
    }

    public static void add(HttpServletRequest req, String filename) {
        HttpSession session = req.getSession(true);
        synchronized (session) {
            set(session).add(filename);
        }
    }

    public static boolean contains(HttpServletRequest req, String filename) {
        HttpSession session = req.getSession(false);
        if (session == null || filename == null) {
            return false;
        }
        synchronized (session) {
            return set(session).contains(filename);
        }
    }

    public static void remove(HttpServletRequest req, String filename) {
        HttpSession session = req.getSession(false);
        if (session == null || filename == null) {
            return;
        }
        synchronized (session) {
            set(session).remove(filename);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> set(HttpSession session) {
        Object existing = session.getAttribute(ATTRIBUTE);
        if (existing == null) {
            existing = new HashSet<String>();
            session.setAttribute(ATTRIBUTE, existing);
        }
        return (Set<String>) existing;
    }
}
