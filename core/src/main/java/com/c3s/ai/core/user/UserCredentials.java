package com.c3s.ai.core.user;

/**
 * A uwnefl_users row as sign-in needs it, hash included. Package-private: only AuthService holds
 * one, and what leaves this package is an AuthenticatedUser.
 */
record UserCredentials(long id, String username, String passwordHash, String displayName, Role role) {
}
