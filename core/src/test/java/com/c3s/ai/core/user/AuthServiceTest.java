package com.c3s.ai.core.user;

import com.c3s.ai.core.auth.LoginThrottle;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    // low cost keeps the tests fast
    private static final String HASH = BCrypt.hashpw("s3cret", BCrypt.gensalt(4));
    private static final String CLIENT = "203.0.113.7";

    /**
     * Stands in for uwnefl_users with MySQL's default collation: "user", "USER", "\u00fcser"
     * (u with umlaut) all find the one row, and the row's own spelling comes back.
     */
    private static UserCredentialsDao oneUser(String storedHash, Role role) {
        return typed -> {
            String folded = java.text.Normalizer.normalize(typed, java.text.Normalizer.Form.NFKD)
                    .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
            return "user".equals(folded)
                    ? Optional.of(new UserCredentials(7, "user", storedHash, "Demo U.", role))
                    : Optional.empty();
        };
    }

    private static AuthService service(String storedHash, Role role) {
        return new AuthService(oneUser(storedHash, role), new LoginThrottle(5), new LoginThrottle(20));
    }

    private final AuthService service = service(HASH, Role.USER);

    @Test
    void acceptsTheRightPasswordAndReturnsTheUserWithoutAHash() {
        LoginResult result = service.login("user", "s3cret", CLIENT);
        AuthenticatedUser user = assertInstanceOf(LoginResult.Success.class, result).user();
        assertEquals(new AuthenticatedUser("user", "Demo U.", Role.USER), user);
    }

    @Test
    void trimsTheUsername() {
        assertInstanceOf(LoginResult.Success.class, service.login("  user ", "s3cret", CLIENT));
    }

    @Test
    void rejectsAWrongPasswordAnUnknownUserAndMissingInputAlike() {
        assertInstanceOf(LoginResult.Rejected.class, service.login("user", "nope", CLIENT));
        assertInstanceOf(LoginResult.Rejected.class, service.login("someone", "s3cret", CLIENT));
        assertInstanceOf(LoginResult.Rejected.class, service.login(null, "s3cret", CLIENT));
        assertInstanceOf(LoginResult.Rejected.class, service.login("user", null, CLIENT));
    }

    @Test
    void aDonorRecordWithoutAPasswordCannotSignIn() {
        AuthService noLogin = service(null, Role.USER);
        assertInstanceOf(LoginResult.Rejected.class, noLogin.login("user", "", CLIENT));
        assertInstanceOf(LoginResult.Rejected.class, noLogin.login("user", "anything", CLIENT));
    }

    @Test
    void aBrokenStoredHashIsARejectNotACrash() {
        assertInstanceOf(LoginResult.Rejected.class, service("not-a-bcrypt-hash", Role.USER).login("user", "s3cret", CLIENT));
    }

    // One credential check for both sign-in pages: the role rides along on the result and
    // the callers decide what it opens, so this must report it faithfully.
    @Test
    void carriesTheRoleThroughUnchanged() {
        for (Role role : Role.values()) {
            LoginResult result = service(HASH, role).login("user", "s3cret", CLIENT);
            assertEquals(role, assertInstanceOf(LoginResult.Success.class, result).user().role());
        }
    }

    @Test
    void theAccountLocksAfterFiveAttemptsAndThePasswordIsNoLongerChecked() {
        for (int i = 0; i < 5; i++) {
            assertInstanceOf(LoginResult.Rejected.class, service.login("user", "wrong", CLIENT));
        }
        assertInstanceOf(LoginResult.Throttled.class, service.login("user", "s3cret", CLIENT));
    }

    // every spelling the database treats as the same username shares one bucket
    @Test
    void caseAndAccentVariantsShareTheAccountsBucket() {
        String[] variants = {"user", "USER", "\u00fcser", "us\u00e9r", " \u00dcSER "};
        for (String variant : variants) {
            assertInstanceOf(LoginResult.Rejected.class, service.login(variant, "wrong", CLIENT));
        }
        assertInstanceOf(LoginResult.Throttled.class, service.login("user", "s3cret", CLIENT));
    }

    @Test
    void aSuccessClearsTheAccountAndDoesNotCountAgainstTheClient() {
        LoginThrottle byClient = new LoginThrottle(3);
        AuthService tight = new AuthService(oneUser(HASH, Role.USER), new LoginThrottle(5), byClient);
        for (int i = 0; i < 10; i++) {
            assertInstanceOf(LoginResult.Success.class, tight.login("user", "s3cret", CLIENT));
        }
        assertTrue(!byClient.isLocked(CLIENT));
    }

    // attempts are counted before BCrypt runs, so a parallel burst can't exceed the limit
    @Test
    void parallelAttemptsCannotExceedTheLimit() throws Exception {
        AuthService parallel = service(HASH, Role.USER);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<LoginResult>> results = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                results.add(pool.submit(() -> parallel.login("user", "wrong", CLIENT)));
            }
            long checked = 0;
            for (Future<LoginResult> f : results) {
                if (f.get() instanceof LoginResult.Rejected) {
                    checked++;
                }
            }
            assertEquals(5, checked, "only five passwords may be checked");
        } finally {
            pool.shutdownNow();
        }
    }
}
