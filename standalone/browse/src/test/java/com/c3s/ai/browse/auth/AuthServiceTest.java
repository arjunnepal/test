package com.c3s.ai.browse.auth;

import com.c3s.ai.browse.model.User;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    // low cost keeps the tests fast
    private static final String HASH = BCrypt.hashpw("s3cret", BCrypt.gensalt(4));

    private static AuthService serviceWithHash(String storedHash) {
        return new AuthService(username -> {
            if (!"user".equals(username)) {
                return Optional.empty();
            }
            User user = new User();
            user.setUsername("user");
            user.setPasswordHash(storedHash);
            user.setDisplayName("Demo U.");
            return Optional.of(user);
        });
    }

    private final AuthService service = serviceWithHash(HASH);

    @Test
    void acceptsTheRightPasswordAndReturnsTheProfile() {
        User user = service.authenticate("user", "s3cret").orElseThrow();
        assertEquals("user", user.getUsername());
        assertEquals("Demo U.", user.getDisplayName());
    }

    @Test
    void trimsTheUsername() {
        assertTrue(service.authenticate("  user ", "s3cret").isPresent());
    }

    @Test
    void rejectsAWrongPassword() {
        assertTrue(service.authenticate("user", "nope").isEmpty());
    }

    @Test
    void rejectsAnUnknownUser() {
        assertTrue(service.authenticate("someone", "s3cret").isEmpty());
    }

    @Test
    void rejectsMissingInput() {
        assertTrue(service.authenticate(null, "s3cret").isEmpty());
        assertTrue(service.authenticate("user", null).isEmpty());
    }

    @Test
    void aDonorRecordWithoutAPasswordCannotSignIn() {
        assertTrue(serviceWithHash(null).authenticate("user", "").isEmpty());
        assertTrue(serviceWithHash(null).authenticate("user", "anything").isEmpty());
    }

    @Test
    void aBrokenStoredHashIsARejectNotACrash() {
        assertTrue(serviceWithHash("not-a-bcrypt-hash").authenticate("user", "s3cret").isEmpty());
    }
}
