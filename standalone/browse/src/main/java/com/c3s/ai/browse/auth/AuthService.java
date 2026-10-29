package com.c3s.ai.browse.auth;

import com.c3s.ai.browse.dao.JdbcUserDao;
import com.c3s.ai.browse.dao.UserDao;
import com.c3s.ai.browse.model.User;
import com.c3s.ai.browse.util.Log;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// Checks browse logins against uwnefl_browse_users.
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int BCRYPT_COST = 12;

    // checked for unknown usernames too, so the response time doesn't give them away
    private static final String DUMMY_HASH = BCrypt.hashpw("timing-equalizer", BCrypt.gensalt(BCRYPT_COST));

    private final UserDao userDao;

    public AuthService() {
        this(new JdbcUserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        Optional<User> found = userDao.findByUsername(username.trim());
        if (found.isEmpty()) {
            BCrypt.checkpw(password, DUMMY_HASH);
            return Optional.empty();
        }
        User user = found.get();
        if (user.getPasswordHash() == null) {
            // donor record without a login
            BCrypt.checkpw(password, DUMMY_HASH);
            return Optional.empty();
        }
        boolean ok;
        try {
            ok = BCrypt.checkpw(password, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            // corrupt hash in the DB
            log.error("Stored password hash for username={} is not a valid BCrypt hash", Log.clean(username), e);
            return Optional.empty();
        }
        return ok ? Optional.of(user) : Optional.empty();
    }
}
