package com.c3s.ai.core.user;

/** A user whose password has just been checked. Deliberately carries no password hash. */
public record AuthenticatedUser(String username, String displayName, Role role) {
}
