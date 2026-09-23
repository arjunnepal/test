package com.c3s.ai.core.user;

import java.util.Locale;

/**
 * What a user is allowed to do, stored as uwnefl_users.role. ADMINISTRATOR gets the whole
 * dashboard, EMPLOYEE its day-to-day profile pages, USER the public site only. To add a role, add
 * it here and to the column's ENUM; the route rules live in AuthFilter.
 */
public enum Role {

    ADMINISTRATOR,
    EMPLOYEE,
    USER;

    /** Anything that may reach /admin at all. */
    public boolean isStaff() {
        return this == ADMINISTRATOR || this == EMPLOYEE;
    }

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }

    /** "Administrator", "Employee", "User". */
    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    /** Null-safe: anything unrecognised is the least-privileged role, never a guess upward. */
    public static Role fromDb(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
