package com.c3s.ai.core.user;

import java.util.Optional;

/** Package-private on purpose - nothing outside core.user can read a password hash. */
interface UserCredentialsDao {

    /** Empty when there's no such row. Matching follows the column's collation (case- and accent-insensitive). */
    Optional<UserCredentials> findByUsername(String username);
}
