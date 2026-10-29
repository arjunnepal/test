-- Schema. Run this against your MySQL instance before deploying the app:
--   mysql -h <host> -u <user> -p < db/schema.sql
--
-- All tables share the uwnefl_ prefix so they can coexist with other
-- applications' tables in a shared database.

-- Admins who can sign in. No self-service sign-up; seed / manage rows directly.
-- Passwords are BCrypt hashes.
CREATE TABLE IF NOT EXISTS uwnefl_users (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100)  NOT NULL UNIQUE,
    password_hash VARCHAR(100)  NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default admin: "admin" / "admin123" - Local setup only.
INSERT IGNORE INTO uwnefl_users (username, password_hash) VALUES
    ('admin', '$2a$12$G91bt3bRybpsP379TP79G.GStYw10jA0L2DJzSySjEMVjaQZhFuOG');

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

INSERT IGNORE INTO uwnefl_categories (label, emoji, color_class) VALUES
    ('Education',        '🎓', 'cat-blue'),
    ('Health',           '🏥', 'cat-red'),
    ('Housing',          '🏠', 'cat-gold'),
    ('Food & Nutrition', '🥗', 'cat-green'),
    ('Veterans',         '🎖️', 'cat-purple'),
    ('Other',            '✦', 'cat-neutral');

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
    -- Every list query is `... WHERE status <> 'archived' ORDER BY created_at DESC`
    -- (or the same without the filter); this covers both without a filesort.
    INDEX uwnefl_idx_profiles_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A profile can belong to more than one category.
-- ON DELETE is asymmetric on purpose: deleting a profile cleans up its category
-- links (CASCADE), but a category still referenced by a profile can't be deleted
-- out from under it (RESTRICT) - archive it instead.
CREATE TABLE IF NOT EXISTS uwnefl_profile_categories (
    profile_id  BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (profile_id, category_id),
    CONSTRAINT uwnefl_fk_pc_profile  FOREIGN KEY (profile_id)  REFERENCES uwnefl_profiles(id)   ON DELETE CASCADE,
    CONSTRAINT uwnefl_fk_pc_category FOREIGN KEY (category_id) REFERENCES uwnefl_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One default policy (category_id NULL) plus an optional override per category.
CREATE TABLE IF NOT EXISTS uwnefl_allocation_settings (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id         BIGINT UNSIGNED NULL UNIQUE,
    fee_model           ENUM('deducted','added','absorbed') NOT NULL DEFAULT 'deducted',
    fee_pct             DECIMAL(4,2)    NOT NULL DEFAULT 2.90,
    fee_fixed           DECIMAL(4,2)    NOT NULL DEFAULT 0.30,
    allow_donor_adjust  TINYINT(1)      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uwnefl_fk_as_category FOREIGN KEY (category_id) REFERENCES uwnefl_categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO uwnefl_allocation_settings (id, category_id) VALUES (1, NULL);

-- Buckets belong to one policy (the default row, or a category's override row).
CREATE TABLE IF NOT EXISTS uwnefl_allocation_buckets (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    settings_id     BIGINT UNSIGNED NOT NULL,
    bucket_key      VARCHAR(40)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    pct             TINYINT UNSIGNED NOT NULL,
    to_recipient    TINYINT(1)      NOT NULL DEFAULT 0,
    core            TINYINT(1)      NOT NULL DEFAULT 0,
    description     VARCHAR(255)    NULL,
    purpose         VARCHAR(255)    NULL,
    sort_order      INT UNSIGNED    NOT NULL DEFAULT 0,

    CONSTRAINT uwnefl_fk_ab_settings FOREIGN KEY (settings_id) REFERENCES uwnefl_allocation_settings(id) ON DELETE CASCADE,
    CONSTRAINT uwnefl_uq_ab_settings_key UNIQUE (settings_id, bucket_key),
    CONSTRAINT uwnefl_chk_ab_pct CHECK (pct BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO uwnefl_allocation_buckets (settings_id, bucket_key, name, pct, to_recipient, core, description, sort_order) VALUES
    (1, 'restricted',   'Restricted',   85, 1, 1, 'Held for the named recipient - cannot be spent elsewhere', 1),
    (1, 'unrestricted', 'Unrestricted', 15, 0, 1, 'UWNEFL general fund - programs, staffing, operations', 2);
