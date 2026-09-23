package com.c3s.ai.core.user;

/** The outcome of one sign-in attempt. Sealed, so a caller's switch has to handle all three. */
public sealed interface LoginResult {

    record Success(AuthenticatedUser user) implements LoginResult {
    }

    /** Unknown user, no password on the account, or the wrong password - deliberately indistinguishable. */
    record Rejected() implements LoginResult {
    }

    /** Too many recent attempts for this account or client; the password was not checked. */
    record Throttled() implements LoginResult {
    }
}
