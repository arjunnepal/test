-- Demo data for local development. Not packaged into the WAR (see pom packagingExcludes).
-- Run right after schema.sql, which drops and recreates every table.

-- One signed-in account per role, so every rule in AuthFilter is exercisable locally.
-- Each password is the username + "123". LOCAL ONLY - never deploy these rows.
--   admin    / admin123     ADMINISTRATOR  (both admin pages and the giving page)
--   employee / employee123  EMPLOYEE       (profiles; 403 on categories/allocations)
--   user     / user123      USER           (giving page; redirected away from /admin)
-- jordan has no password_hash at all - a donor record that can receive donations but
-- cannot sign in, which is the case AuthService has to handle without a NullPointerException.
INSERT INTO uwnefl_users (username, password_hash, display_name, role) VALUES
    ('admin',    '$2a$12$G91bt3bRybpsP379TP79G.GStYw10jA0L2DJzSySjEMVjaQZhFuOG', 'Administrator',  'ADMINISTRATOR'),
    ('employee', '$2a$12$EmmJu6i.Q.h8A75kXZH23.T7kOx/rhh4ayipgFhVKmjssrfV874J6', 'Casework Staff', 'EMPLOYEE'),
    ('user',     '$2a$12$NsHKrqlXIGYKmXokbMDD7.L/iihKy6TDrSl2Nmg1WJJUjK8eQWftq', 'Demo U.',        'USER'),
    ('jordan',   NULL,                                                           'Jordan M.',      'USER');

-- Demo profiles
INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
VALUES
    ('Marcus T.', 'Jacksonville', 'FL',
     'First-generation nursing student needing tuition and textbook support to finish his degree.',
     'Marcus is the first in his family to attend college. A working single dad, he juggles night shifts at a local grocery store with full-time coursework in the RN program at Florida State College at Jacksonville. His goal is clear: graduate debt-free, serve his community as a nurse, and show his daughter that anything is possible.',
     '🎓', 2000, 1200, '2026-07-31', 1, 1, 1, 1, 'published'),
    ('Diane & Ray K.', 'Jacksonville', 'FL',
     'Elderly couple with mounting medical bills after Ray''s stroke. Insurance covered only part of rehab.',
     'Ray suffered a severe ischemic stroke in February. After 6 weeks of in-patient rehab, he returned home but requires outpatient physical and speech therapy that their Medicare supplement refuses to cover. Diane, 72, is his full-time caregiver while managing her own heart condition. Every dollar raised pays directly for Ray''s therapy sessions.',
     '🏥', 6000, 4100, '2026-06-15', 1, 1, 1, 1, 'published'),
    ('Sophia L.', 'Jacksonville', 'FL',
     'Single mother of two who lost her apartment in a fire. Working toward stable housing.',
     'Sophia is a single mother of two young children who lost her apartment in a devastating fire earlier this year. She and her kids spent the first two months in an emergency shelter while she searched for stable housing. She currently works part-time as a home health aide and studies for her LPN license on weekends. Help her secure a deposit and first month''s rent on a new apartment.',
     '🏠', 3500, 900, '2026-06-30', 1, 1, 1, 1, 'published'),
    ('The Rivera Family', 'Jacksonville', 'FL',
     'Family of five facing food insecurity after job loss. Working with local pantry but need support.',
     'Carlos Rivera was laid off from his warehouse job of 11 years when the facility closed. His wife Lucinda works part-time, but with three school-age children and a grandmother living with them, the family''s grocery budget simply doesn''t stretch far enough. They visit the local food pantry weekly but need supplemental support for fresh produce and protein.',
     '🥗', 800, 650, '2026-05-31', 1, 1, 1, 1, 'published'),
    ('James W.', 'Jacksonville', 'FL',
     'Army veteran navigating PTSD and housing instability. Seeking counseling and rental assistance.',
     'James served two tours in Afghanistan and returned home with severe PTSD and a traumatic brain injury. After a difficult few years, he connected with the VA but is still on a waiting list for housing assistance. He needs help with three months of rental assistance while he completes his counseling program and gets back on his feet. James wants to become a peer support specialist to help other veterans.',
     '🎖️', 4000, 2800, '2026-08-31', 1, 1, 1, 1, 'published'),
    ('Amara O.', 'Jacksonville', 'FL',
     'Refugee student with exceptional grades pursuing a computer science scholarship at community college.',
     'Amara arrived in Jacksonville from Somalia three years ago with her mother and two younger siblings. She earned a 3.98 GPA in high school and was accepted into the Computer Science program at Florida State College at Jacksonville. A small scholarship covers tuition, but she needs help with her laptop, internet access, textbooks, and transportation. Her dream is to become a software engineer.',
     '📚', 1500, 500, '2026-08-15', 1, 1, 1, 1, 'published'),
    ('Dr. Keisha B.', 'Jacksonville', 'FL',
     'Community doctor battling cancer while running a free clinic for uninsured residents.',
     'Dr. Keisha has operated a free medical clinic in the Northside neighborhood for 12 years. Last fall she was diagnosed with Stage 2 breast cancer. Her treatment costs are draining the clinic''s operating reserve, putting both her care and her patients'' access to healthcare at risk. Your donation directly supports Dr. Keisha''s treatment and keeps the clinic open for over 300 uninsured patients who rely on it.',
     '🩺', 8000, 5200, '2026-09-30', 1, 1, 1, 1, 'published'),
    ('The Nguyen Family', 'Jacksonville', 'FL',
     'Vietnamese immigrant family of four facing eviction after predatory landlord doubled their rent.',
     'Minh and Lan Nguyen immigrated to Jacksonville 8 years ago. They have two children in elementary school and Minh works as a mechanic while Lan is a home healthcare aide. Their landlord doubled the rent with 30 days notice — a tactic that is unfortunately legal in Florida. They are working with a housing attorney but need funds to secure a new apartment deposit and moving costs while the case is reviewed.',
     '🏡', 5000, 2100, '2026-07-15', 1, 1, 1, 1, 'published'),
    ('Rosa & Miguel D.', 'Jacksonville', 'FL',
     'Elderly couple living on fixed income; facing rising grocery costs and transportation barriers.',
     'Rosa (78) and Miguel (81) Dominguez have lived in the same Northeast Jacksonville neighborhood for 40 years. Their combined Social Security income of $1,640/month barely covers rent and utilities. With no car and the nearest grocery store 3 miles away, they rely on a volunteer-driven meal program that recently reduced its service days. Your support pays for grocery delivery and supplemental meals.',
     '🌽', 600, 480, '2026-05-31', 1, 1, 1, 1, 'published'),
    ('Sgt. Patricia H.', 'Jacksonville', 'FL',
     'Female Marine sergeant, single parent, navigating VA backlog for disability benefits.',
     'Patricia served 12 years as a Marine Corps logistics sergeant, including deployments to Iraq and Bahrain. She returned home with hearing loss and a spinal injury sustained in a vehicle accident during her service. Her VA disability claim was filed 14 months ago and remains in the backlog. In the meantime, she''s supporting her 9-year-old son on a part-time salary. Your donation helps cover rent, medication co-pays, and childcare while her claim is processed.',
     '🏅', 5500, 3900, '2026-10-31', 1, 1, 1, 1, 'published');

-- Category links. Categories are 1 Education, 2 Health, 3 Housing, 4 Food & Nutrition,
-- 5 Veterans (schema.sql), profiles 1-10 in the order above.
INSERT INTO uwnefl_profile_categories (profile_id, category_id) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5),
    (6, 1), (7, 2), (8, 3), (9, 4), (10, 5);

-- Donors from "This Week's Donors". With the fresh tables from schema.sql they get ids 5-12
-- after the four accounts above, and the profiles ids 1-10 in the order they're inserted.
INSERT INTO uwnefl_users (username, display_name, role) VALUES
    ('cwashington', 'C. Washington',  'USER'),
    ('rfitzgerald', 'R. Fitzgerald',  'USER'),
    ('okafor',      'L. & T. Okafor', 'USER'),
    ('jmartinez',   'J. Martinez',    'USER'),
    ('pchen',       'P. Chen',        'USER'),
    ('kimfamily',   'The Kim Family', 'USER'),
    ('gpatel',      'G. Patel',       'USER'),
    ('mthompson',   'M. Thompson',    'USER');

-- Every donation is settled against the default policy (id 1): fee deducted at 2.90% + $0.30,
-- 15% of the net unrestricted. The amounts are worked out already.

-- Today, a minute apart and newest first. NULL user_id = anonymous.
INSERT INTO uwnefl_donations
    (profile_id, user_id, amount, frequency, donated_at, settings_id, fee_model, fee_pct, fee_fixed,
     unrestricted_pct, charge_amount, fee_amount, processing_amount, restricted_amount, unrestricted_amount)
VALUES
    (10,    5,  2500, 'one_time', NOW() - INTERVAL  1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  2500.00,   72.80,   72.80,  2063.12,  364.08),
    ( 2, NULL,  1800, 'one_time', NOW() - INTERVAL  2 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  1800.00,   52.50,   52.50,  1485.37,  262.13),
    ( 1,    6,  1200, 'monthly',  NOW() - INTERVAL  3 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  1200.00,   35.10,   35.10,   990.16,  174.74),
    ( 6,    7,   900, 'one_time', NOW() - INTERVAL  4 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   900.00,   26.40,   26.40,   742.56,  131.04),
    ( 5, NULL,   750, 'one_time', NOW() - INTERVAL  5 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   750.00,   22.05,   22.05,   618.76,  109.19),
    ( 3,    8,   500, 'monthly',  NOW() - INTERVAL  6 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   500.00,   14.80,   14.80,   412.42,   72.78),
    ( 4,    9,   400, 'one_time', NOW() - INTERVAL  7 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   400.00,   11.90,   11.90,   329.88,   58.22),
    ( 7, NULL,   350, 'one_time', NOW() - INTERVAL  8 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   350.00,   10.45,   10.45,   288.62,   50.93),
    ( 8,   10,   300, 'one_time', NOW() - INTERVAL  9 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   300.00,    9.00,    9.00,   247.35,   43.65),
    ( 9,   11,   250, 'one_time', NOW() - INTERVAL 10 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   250.00,    7.55,    7.55,   206.08,   36.37),
    ( 1,   12,   200, 'one_time', NOW() - INTERVAL 11 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   200.00,    6.10,    6.10,   164.81,   29.09),
    ( 3, NULL,   150, 'one_time', NOW() - INTERVAL 12 MINUTE, 1, 'deducted', 2.90, 0.30, 15,   150.00,    4.65,    4.65,   123.55,   21.80);

-- History for the financial report: a gift on today's weekday in each of the eleven weeks before
-- this one, one per bar of the weekly chart. The extra minute keeps the newest out of the giving
-- page's last-7-days ticker. The last two, a year back, are the year-on-year comparison, sized so
-- the badge reads 18.4% like the reference - from mid-March on; seeded earlier, the oldest weeks
-- fall in last year and the figure differs.
INSERT INTO uwnefl_donations
    (profile_id, user_id, amount, frequency, donated_at, settings_id, fee_model, fee_pct, fee_fixed,
     unrestricted_pct, charge_amount, fee_amount, processing_amount, restricted_amount, unrestricted_amount)
VALUES
    ( 5,    5,  7200, 'one_time', NOW() - INTERVAL  7 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  7200.00,  209.10,  209.10,  5942.26, 1048.64),
    ( 6,   12,  8400, 'one_time', NOW() - INTERVAL 14 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  8400.00,  243.90,  243.90,  6932.68, 1223.42),
    ( 7,   11,  6100, 'monthly',  NOW() - INTERVAL 21 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  6100.00,  177.20,  177.20,  5034.38,  888.42),
    ( 8,   10,  9300, 'one_time', NOW() - INTERVAL 28 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  9300.00,  270.00,  270.00,  7675.50, 1354.50),
    ( 9,    9,  5600, 'one_time', NOW() - INTERVAL 35 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  5600.00,  162.70,  162.70,  4621.70,  815.60),
    (10,    8,  7800, 'monthly',  NOW() - INTERVAL 42 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  7800.00,  226.50,  226.50,  6437.47, 1136.03),
    ( 1,    7,  6700, 'one_time', NOW() - INTERVAL 49 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  6700.00,  194.60,  194.60,  5529.59,  975.81),
    ( 2,    6,  8900, 'one_time', NOW() - INTERVAL 56 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  8900.00,  258.40,  258.40,  7345.36, 1296.24),
    ( 3,    5,  5200, 'one_time', NOW() - INTERVAL 63 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  5200.00,  151.10,  151.10,  4291.56,  757.34),
    ( 4,   12,  7400, 'monthly',  NOW() - INTERVAL 70 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  7400.00,  214.90,  214.90,  6107.33, 1077.77),
    ( 5,   11,  6300, 'one_time', NOW() - INTERVAL 77 DAY - INTERVAL 1 MINUTE, 1, 'deducted', 2.90, 0.30, 15,  6300.00,  183.00,  183.00,  5199.45,  917.55),
    ( 6,   10, 40000, 'one_time', NOW() - INTERVAL 1 YEAR, 1, 'deducted', 2.90, 0.30, 15, 40000.00, 1160.30, 1160.30, 33013.74, 5825.96),
    ( 8,    9, 34500, 'monthly',  NOW() - INTERVAL 1 YEAR - INTERVAL 3 DAY, 1, 'deducted', 2.90, 0.30, 15, 34500.00, 1000.80, 1000.80, 28474.32, 5024.88);
