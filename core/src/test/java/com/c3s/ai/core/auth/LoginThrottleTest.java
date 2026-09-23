package com.c3s.ai.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleTest {

    private static void attempts(LoginThrottle throttle, String key, int times) {
        for (int i = 0; i < times; i++) {
            assertTrue(throttle.tryAcquire(key), "attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void theAttemptThatReachesTheLimitIsAllowedAndLocksBehindIt() {
        LoginThrottle throttle = new LoginThrottle(3);
        attempts(throttle, "user", 2);
        assertFalse(throttle.isLocked("user"));

        assertTrue(throttle.tryAcquire("user"));
        assertTrue(throttle.isLocked("user"));
        assertFalse(throttle.tryAcquire("user"));
    }

    @Test
    void keysFoldCaseSpacesAndAccents() {
        assertEquals("jose", LoginThrottle.normalize("  Jos\u00e9 "));
        assertEquals("admin", LoginThrottle.normalize("\u00c1DMIN"));
        assertEquals("fi", LoginThrottle.normalize("\ufb01"));   // the "fi" ligature

        LoginThrottle throttle = new LoginThrottle(3);
        throttle.tryAcquire("admin");
        throttle.tryAcquire("ADMIN");
        throttle.tryAcquire("\u00e1dmin");
        assertTrue(throttle.isLocked(" Admin "));
    }

    @Test
    void releaseGivesBackTheLastAttemptAndItsLock() {
        LoginThrottle throttle = new LoginThrottle(3);
        attempts(throttle, "10.0.0.1", 3);
        assertTrue(throttle.isLocked("10.0.0.1"));

        throttle.release("10.0.0.1");

        assertFalse(throttle.isLocked("10.0.0.1"));
        assertTrue(throttle.tryAcquire("10.0.0.1"));
    }

    @Test
    void resetClearsTheLock() {
        LoginThrottle throttle = new LoginThrottle(3);
        attempts(throttle, "user", 3);
        throttle.reset("user");
        assertFalse(throttle.isLocked("user"));
    }

    @Test
    void locksAreSeparatePerKey() {
        LoginThrottle throttle = new LoginThrottle(3);
        attempts(throttle, "10.0.0.1", 3);
        assertTrue(throttle.isLocked("10.0.0.1"));
        assertFalse(throttle.isLocked("10.0.0.2"));
    }

    @Test
    void theSharedThrottlesUseTheDocumentedLimits() {
        assertEquals(5, LoginThrottle.ACCOUNT_MAX_ATTEMPTS);
        assertEquals(20, LoginThrottle.CLIENT_MAX_ATTEMPTS);
    }
}
