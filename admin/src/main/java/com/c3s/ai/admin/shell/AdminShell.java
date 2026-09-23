package com.c3s.ai.admin.shell;

/** What admin/common/header.jspf shows around every dashboard page: who's signed in, and the sidebar numbers. */
public record AdminShell(String username, String initials, String roleLabel, boolean administrator,
                         long publishedProfiles, long raisedTotal) {

    static String initialsOf(String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        String u = username.trim();
        return (u.length() >= 2 ? u.substring(0, 2) : u).toUpperCase(java.util.Locale.ROOT);
    }
}
