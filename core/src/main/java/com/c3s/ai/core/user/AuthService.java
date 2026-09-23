package com.c3s.ai.core.user;

import com.c3s.ai.common.util.Log;
import com.c3s.ai.common.util.Strings;
import com.c3s.ai.core.auth.LoginThrottle;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Sign-in for both doors: throttling plus the BCrypt check against uwnefl_users. The password
 * hash never leaves this package - a success carries an AuthenticatedUser.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int BCRYPT_COST = 12;

    // checked whenever there's no real hash to check, so the response time doesn't reveal
    // whether the username exists, has no login, or has a corrupt hash
    private static final String DUMMY_HASH = BCrypt.hashpw("timing-equalizer", BCrypt.gensalt(BCRYPT_COST));

    private final UserCredentialsDao users;
    private final LoginThrottle byAccount;
    private final LoginThrottle byClient;

    public AuthService() {
        this(new JdbcUserCredentialsDao(), LoginThrottle.BY_ACCOUNT, LoginThrottle.BY_CLIENT);
    }

    // tests hand in a stub DAO and their own throttles
    AuthService(UserCredentialsDao users, LoginThrottle byAccount, LoginThrottle byClient) {
        this.users = users;
        this.byAccount = byAccount;
        this.byClient = byClient;
    }

    public LoginResult login(String username, String password, String clientAddress) {
        String typed = Strings.trimToNull(username);
        Optional<UserCredentials> found = typed == null ? Optional.empty() : users.findByUsername(typed);

        // Throttle on the account the database actually matched, so every spelling MySQL treats
        // as the same username shares one bucket.
        String accountKey = found.map(UserCredentials::username).orElse(typed);
        if (!byAccount.tryAcquire(accountKey)) {
            return new LoginResult.Throttled();
        }
        if (!byClient.tryAcquire(clientAddress)) {
            byAccount.release(accountKey);
            return new LoginResult.Throttled();
        }

        if (password == null || found.isEmpty() || found.get().passwordHash() == null) {
            checkDummy(password);
            return new LoginResult.Rejected();
        }
        UserCredentials creds = found.get();
        boolean ok;
        try {
            ok = BCrypt.checkpw(password, creds.passwordHash());
        } catch (IllegalArgumentException e) {
            log.error("Stored password hash for username={} is not a valid BCrypt hash", Log.clean(creds.username()));
            checkDummy(password);
            return new LoginResult.Rejected();
        }
        if (!ok) {
            return new LoginResult.Rejected();
        }

        byAccount.reset(accountKey);
        byClient.release(clientAddress);
        return new LoginResult.Success(new AuthenticatedUser(creds.username(), creds.displayName(), creds.role()));
    }

    private static void checkDummy(String password) {
        BCrypt.checkpw(password == null ? "" : password, DUMMY_HASH);
    }
}
