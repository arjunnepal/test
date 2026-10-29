-- Tables used by browse. Run on the same database as the admin app:
--   mysql --default-character-set=utf8mb4 -h <host> -u <user> -p <db> < src/main/resources/db/browse-schema.sql
-- No data here, demo rows are in browse-seed.sql.

-- Admin's tables, copied from admin's schema.sql so this script works on its own. Either script can
-- run first. If admin changes one of these, update it here too.

CREATE TABLE IF NOT EXISTS uwnefl_categories (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    label       VARCHAR(100)  NOT NULL UNIQUE,
    emoji       VARCHAR(8)    NULL,
    color_class VARCHAR(20)   NOT NULL DEFAULT 'cat-blue',
    status      ENUM('active','archived') NOT NULL DEFAULT 'active',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uwnefl_chk_categories_color CHECK (color_class IN
('cat-blue','cat-red','cat-gold','cat-green','cat-purple','cat-neutral'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS uwnefl_profiles (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    city                VARCHAR(100)    NULL,
    state               VARCHAR(2)      NULL,
    blurb               VARCHAR(160)    NOT NULL,
    story               VARCHAR(2000)   NULL,
    emoji               VARCHAR(8)      NULL,
    photo_filename      VARCHAR(80)     NULL,
    goal                INT UNSIGNED    NOT NULL,
    raised              INT UNSIGNED    NOT NULL DEFAULT 0,
    deadline            DATE            NULL,
    identity_verified   TINYINT(1)      NOT NULL DEFAULT 0,
    needs_reviewed      TINYINT(1)      NOT NULL DEFAULT 0,
    paypal_confirmed    TINYINT(1)      NOT NULL DEFAULT 0,
    story_reviewed      TINYINT(1)      NOT NULL DEFAULT 0,
    status              ENUM('draft','published','archived') NOT NULL DEFAULT 'draft',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uwnefl_chk_profiles_raised_le_goal CHECK (raised <= goal),
    INDEX uwnefl_idx_profiles_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- a profile can be in more than one category
CREATE TABLE IF NOT EXISTS uwnefl_profile_categories (
    profile_id  BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (profile_id, category_id),
    CONSTRAINT uwnefl_fk_pc_profile  FOREIGN KEY (profile_id)  REFERENCES uwnefl_profiles(id)   ON DELETE CASCADE,
    CONSTRAINT uwnefl_fk_pc_category FOREIGN KEY (category_id) REFERENCES uwnefl_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Browse's own tables

-- Browse accounts, separate from admin's uwnefl_users.
CREATE TABLE IF NOT EXISTS uwnefl_browse_users (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100)  NOT NULL UNIQUE,
    password_hash VARCHAR(100)  NULL,               -- BCrypt, NULL = no login
    display_name  VARCHAR(100)  NOT NULL,           -- shown publicly
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS uwnefl_donations (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    profile_id  BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NULL,               -- NULL = anonymous
    amount      INT UNSIGNED    NOT NULL,           -- whole dollars
    donated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uwnefl_fk_don_profile FOREIGN KEY (profile_id)
        REFERENCES uwnefl_profiles(id) ON DELETE CASCADE,
    -- deleting a user makes their donations anonymous
    CONSTRAINT uwnefl_fk_don_user FOREIGN KEY (user_id)
        REFERENCES uwnefl_browse_users(id) ON DELETE SET NULL,
    INDEX uwnefl_idx_don_profile (profile_id),
    INDEX uwnefl_idx_don_user (user_id),
    INDEX uwnefl_idx_don_donated_at (donated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
