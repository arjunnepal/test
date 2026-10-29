package com.c3s.ai.browse.auth;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleTest {

    // the shared throttles are static, so use unique keys
    private static String freshKey() {
        return "throttle-test-" + UUID.randomUUID();
    }

    private static void fail(LoginThrottle throttle, String key, int times) {
        for (int i = 0; i < times; i++) {
            throttle.recordFailure(key);
        }
    }

    @Test
    void locksOnlyOnceTheLimitIsReached() {
        LoginThrottle throttle = new LoginThrottle(3);
        fail(throttle, "user", 2);
        assertFalse(throttle.isLocked("user"));

        throttle.recordFailure("user");
        assertTrue(throttle.isLocked("user"));
    }

    @Test
    void keyMatchIgnoresCaseAndSpaces() {
        LoginThrottle throttle = new LoginThrottle(3);
        fail(throttle, "Some.User", 3);
        assertTrue(throttle.isLocked("  some.user "));
    }

    @Test
    void resetClearsTheLock() {
        LoginThrottle throttle = new LoginThrottle(3);
        fail(throttle, "user", 3);
        throttle.reset("user");
        assertFalse(throttle.isLocked("user"));
    }

    @Test
    void locksAreSeparatePerKey() {
        LoginThrottle throttle = new LoginThrottle(3);
        fail(throttle, "10.0.0.1", 3);
        assertTrue(throttle.isLocked("10.0.0.1"));
        assertFalse(throttle.isLocked("10.0.0.2"));
    }

    @Test
    void usernamesLockAfterFiveMisses() {
        String username = freshKey();
        fail(LoginThrottle.BY_USERNAME, username, LoginThrottle.USERNAME_MAX_FAILURES - 1);
        assertFalse(LoginThrottle.BY_USERNAME.isLocked(username));
        LoginThrottle.BY_USERNAME.recordFailure(username);
        assertTrue(LoginThrottle.BY_USERNAME.isLocked(username));
    }

    @Test
    void clientsLockAfterTwentyMisses() {
        String client = freshKey();
        fail(LoginThrottle.BY_CLIENT, client, LoginThrottle.CLIENT_MAX_FAILURES - 1);
        assertFalse(LoginThrottle.BY_CLIENT.isLocked(client));
        LoginThrottle.BY_CLIENT.recordFailure(client);
        assertTrue(LoginThrottle.BY_CLIENT.isLocked(client));
    }
}
