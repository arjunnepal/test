-- Schema for the C3S.ai giving platform - the public giving site and the admin dashboard.
--
--   mysql --default-character-set=utf8mb4 -h <host> -u <user> -p <db> < db/schema.sql
--
-- Every environment is set up clean from this file - there are no migrations. It drops every
-- uwnefl_ table first, so running it against a live database wipes all its data. It inserts no
-- users.
--
-- All tables share the uwnefl_ prefix so they can coexist with other applications'
-- tables in a shared database.

-- children before parents, so the foreign keys never block a drop
DROP TABLE IF EXISTS uwnefl_donations;
DROP TABLE IF EXISTS uwnefl_allocation_buckets;
DROP TABLE IF EXISTS uwnefl_allocation_settings;
DROP TABLE IF EXISTS uwnefl_profile_categories;
DROP TABLE IF EXISTS uwnefl_profiles;
DROP TABLE IF EXISTS uwnefl_categories;
DROP TABLE IF EXISTS uwnefl_users;

-- Everyone who can sign in, plus donors who can't. One table, one role column.
--   ADMINISTRATOR - the whole dashboard, including categories and allocation settings
--   EMPLOYEE      - profiles only, no configuration
--   USER          - the public giving site; also the donor a donation points at
-- No self-service sign-up; seed / manage rows directly. Passwords are BCrypt hashes.
CREATE TABLE uwnefl_users (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NULL,                 -- NULL = donor record with no login
    display_name  VARCHAR(100) NOT NULL,             -- shown publicly
    role          ENUM('ADMINISTRATOR','EMPLOYEE','USER') NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE uwnefl_categories (
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

INSERT INTO uwnefl_categories (label, emoji, color_class) VALUES
    ('Education',        '🎓', 'cat-blue'),
    ('Health',           '🏥', 'cat-red'),
    ('Housing',          '🏠', 'cat-gold'),
    ('Food & Nutrition', '🥗', 'cat-green'),
    ('Veterans',         '🎖️', 'cat-purple'),
    ('Other',            '✦', 'cat-neutral');

CREATE TABLE uwnefl_profiles (
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
CREATE TABLE uwnefl_profile_categories (
    profile_id  BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (profile_id, category_id),
    CONSTRAINT uwnefl_fk_pc_profile  FOREIGN KEY (profile_id)  REFERENCES uwnefl_profiles(id)   ON DELETE CASCADE,
    CONSTRAINT uwnefl_fk_pc_category FOREIGN KEY (category_id) REFERENCES uwnefl_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One default policy (category_id NULL) plus an optional override per category.
CREATE TABLE uwnefl_allocation_settings (
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

INSERT INTO uwnefl_allocation_settings (id, category_id) VALUES (1, NULL);

-- Buckets belong to one policy (the default row, or a category's override row).
CREATE TABLE uwnefl_allocation_buckets (
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

INSERT INTO uwnefl_allocation_buckets (settings_id, bucket_key, name, pct, to_recipient, core, description, sort_order) VALUES
    (1, 'restricted',   'Restricted',   85, 1, 1, 'Held for the named recipient - cannot be spent elsewhere', 1),
    (1, 'unrestricted', 'Unrestricted', 15, 0, 1, 'UWNEFL general fund - programs, staffing, operations', 2);

-- Donations made on the public site. user_id is the donor's uwnefl_users row.
-- The split columns are a snapshot of the allocation policy at settlement, so the financial
-- report doesn't move when a policy is edited later. Whoever records a donation fills them:
--   fee = ROUND(amount * fee_pct / 100 + fee_fixed, 2)
--   deducted: charge = amount,       processing = LEAST(fee, amount), net = amount - processing
--   added:    charge = amount + fee, processing = fee, net = amount
--   absorbed: charge = amount,       processing = 0,   net = amount (UWNEFL pays fee_amount)
--   unrestricted = ROUND(net * unrestricted_pct / 100, 2), restricted = net - unrestricted
-- The CHECK rejects a split that doesn't add back up to the charge.
CREATE TABLE uwnefl_donations (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    profile_id          BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NULL,               -- NULL = anonymous
    amount              INT UNSIGNED    NOT NULL,           -- whole dollars, the gift itself
    frequency           ENUM('one_time','monthly') NOT NULL DEFAULT 'one_time',
    donated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settings_id         BIGINT UNSIGNED NULL,               -- the policy applied at settlement
    fee_model           ENUM('deducted','added','absorbed') NOT NULL,
    fee_pct             DECIMAL(4,2)    NOT NULL,
    fee_fixed           DECIMAL(4,2)    NOT NULL,
    unrestricted_pct    TINYINT UNSIGNED NOT NULL,
    charge_amount       DECIMAL(10,2)   NOT NULL,           -- what the donor paid
    fee_amount          DECIMAL(10,2)   NOT NULL,           -- processor fee, also when absorbed
    processing_amount   DECIMAL(10,2)   NOT NULL,           -- the fee taken from donor money
    restricted_amount   DECIMAL(10,2)   NOT NULL,
    unrestricted_amount DECIMAL(10,2)   NOT NULL,

    CONSTRAINT uwnefl_fk_don_profile FOREIGN KEY (profile_id)
        REFERENCES uwnefl_profiles(id) ON DELETE CASCADE,
    -- deleting a user makes their donations anonymous
    CONSTRAINT uwnefl_fk_don_user FOREIGN KEY (user_id)
        REFERENCES uwnefl_users(id) ON DELETE SET NULL,
    -- the snapshot keeps the numbers if the policy row goes
    CONSTRAINT uwnefl_fk_don_settings FOREIGN KEY (settings_id)
        REFERENCES uwnefl_allocation_settings(id) ON DELETE SET NULL,
    CONSTRAINT uwnefl_chk_don_split
        CHECK (processing_amount + restricted_amount + unrestricted_amount = charge_amount),
    CONSTRAINT uwnefl_chk_don_unres_pct CHECK (unrestricted_pct <= 100),
    INDEX uwnefl_idx_don_profile (profile_id),
    INDEX uwnefl_idx_don_user (user_id),
    INDEX uwnefl_idx_don_donated_at (donated_at),
    -- the financial report's period queries
    INDEX uwnefl_idx_don_donated_profile (donated_at, profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
