package com.c3s.ai.admin.auth;

import com.c3s.ai.admin.dao.JdbcUserDao;
import com.c3s.ai.admin.dao.UserDao;
import com.c3s.ai.admin.model.User;
import com.c3s.ai.admin.util.Log;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/** Checks admin credentials against uwnefl_users (BCrypt). */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final int BCRYPT_COST = 12;

    // checked against an unknown username so that path burns the same time as a real one -
    // no account-existence leak via timing. Same cost as the stored hashes; independent of any user.
    private static final String DUMMY_HASH = BCrypt.hashpw("timing-equalizer", BCrypt.gensalt(BCRYPT_COST));

    private final UserDao userDao;

    public AuthService() {
        this(new JdbcUserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Empty on unknown username OR wrong password - the caller must not tell them apart. */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        Optional<User> found = userDao.findByUsername(username.trim());
        if (found.isEmpty()) {
            BCrypt.checkpw(password, DUMMY_HASH);   // burn the same time, throw away the result
            return Optional.empty();
        }
        User user = found.get();
        boolean ok;
        try {
            ok = BCrypt.checkpw(password, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            // bad hash in the DB - fail the login but shout about it
            log.error("Stored password hash for username={} is not a valid BCrypt hash", Log.clean(username), e);
            return Optional.empty();
        }
        return ok ? Optional.of(user) : Optional.empty();
    }
}
