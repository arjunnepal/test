-- Demo data for local development. Not packaged into the WAR (see pom packagingExcludes).
-- Run after schema.sql.

-- One signed-in account per role, so every rule in AuthFilter is exercisable locally.
-- Each password is the username + "123". LOCAL ONLY - never deploy these rows.
--   admin    / admin123     ADMINISTRATOR  (both admin pages and the giving page)
--   employee / employee123  EMPLOYEE       (profiles; 403 on categories/allocations)
--   user     / user123      USER           (giving page; redirected away from /admin)
-- jordan has no password_hash at all - a donor record that can receive donations but
-- cannot sign in, which is the case AuthService has to handle without a NullPointerException.
INSERT IGNORE INTO uwnefl_users (username, password_hash, display_name, role) VALUES
    ('admin',    '$2a$12$G91bt3bRybpsP379TP79G.GStYw10jA0L2DJzSySjEMVjaQZhFuOG', 'Administrator',  'ADMINISTRATOR'),
    ('employee', '$2a$12$EmmJu6i.Q.h8A75kXZH23.T7kOx/rhh4ayipgFhVKmjssrfV874J6', 'Casework Staff', 'EMPLOYEE'),
    ('user',     '$2a$12$NsHKrqlXIGYKmXokbMDD7.L/iihKy6TDrSl2Nmg1WJJUjK8eQWftq', 'Demo U.',        'USER'),
    ('jordan',   NULL,                                                           'Jordan M.',      'USER');

-- Categories, the same rows admin's schema.sql adds
INSERT IGNORE INTO uwnefl_categories (label, emoji, color_class) VALUES
    ('Education',        '🎓', 'cat-blue'),
    ('Health',           '🏥', 'cat-red'),
    ('Housing',          '🏠', 'cat-gold'),
    ('Food & Nutrition', '🥗', 'cat-green'),
    ('Veterans',         '🎖️', 'cat-purple'),
    ('Other',            '✦', 'cat-neutral');

-- Demo profiles

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Marcus T.', 'Jacksonville', 'FL',
       'First-generation nursing student needing tuition and textbook support to finish his degree.',
       'Marcus is the first in his family to attend college. A working single dad, he juggles night shifts at a local grocery store with full-time coursework in the RN program at Florida State College at Jacksonville. His goal is clear: graduate debt-free, serve his community as a nurse, and show his daughter that anything is possible.',
       '🎓', 2000, 1200, '2026-07-31', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Marcus T.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Diane & Ray K.', 'Jacksonville', 'FL',
       'Elderly couple with mounting medical bills after Ray''s stroke. Insurance covered only part of rehab.',
       'Ray suffered a severe ischemic stroke in February. After 6 weeks of in-patient rehab, he returned home but requires outpatient physical and speech therapy that their Medicare supplement refuses to cover. Diane, 72, is his full-time caregiver while managing her own heart condition. Every dollar raised pays directly for Ray''s therapy sessions.',
       '🏥', 6000, 4100, '2026-06-15', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Diane & Ray K.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Sophia L.', 'Jacksonville', 'FL',
       'Single mother of two who lost her apartment in a fire. Working toward stable housing.',
       'Sophia is a single mother of two young children who lost her apartment in a devastating fire earlier this year. She and her kids spent the first two months in an emergency shelter while she searched for stable housing. She currently works part-time as a home health aide and studies for her LPN license on weekends. Help her secure a deposit and first month''s rent on a new apartment.',
       '🏠', 3500, 900, '2026-06-30', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Sophia L.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'The Rivera Family', 'Jacksonville', 'FL',
       'Family of five facing food insecurity after job loss. Working with local pantry but need support.',
       'Carlos Rivera was laid off from his warehouse job of 11 years when the facility closed. His wife Lucinda works part-time, but with three school-age children and a grandmother living with them, the family''s grocery budget simply doesn''t stretch far enough. They visit the local food pantry weekly but need supplemental support for fresh produce and protein.',
       '🥗', 800, 650, '2026-05-31', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'The Rivera Family');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'James W.', 'Jacksonville', 'FL',
       'Army veteran navigating PTSD and housing instability. Seeking counseling and rental assistance.',
       'James served two tours in Afghanistan and returned home with severe PTSD and a traumatic brain injury. After a difficult few years, he connected with the VA but is still on a waiting list for housing assistance. He needs help with three months of rental assistance while he completes his counseling program and gets back on his feet. James wants to become a peer support specialist to help other veterans.',
       '🎖️', 4000, 2800, '2026-08-31', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'James W.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Amara O.', 'Jacksonville', 'FL',
       'Refugee student with exceptional grades pursuing a computer science scholarship at community college.',
       'Amara arrived in Jacksonville from Somalia three years ago with her mother and two younger siblings. She earned a 3.98 GPA in high school and was accepted into the Computer Science program at Florida State College at Jacksonville. A small scholarship covers tuition, but she needs help with her laptop, internet access, textbooks, and transportation. Her dream is to become a software engineer.',
       '📚', 1500, 500, '2026-08-15', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Amara O.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Dr. Keisha B.', 'Jacksonville', 'FL',
       'Community doctor battling cancer while running a free clinic for uninsured residents.',
       'Dr. Keisha has operated a free medical clinic in the Northside neighborhood for 12 years. Last fall she was diagnosed with Stage 2 breast cancer. Her treatment costs are draining the clinic''s operating reserve, putting both her care and her patients'' access to healthcare at risk. Your donation directly supports Dr. Keisha''s treatment and keeps the clinic open for over 300 uninsured patients who rely on it.',
       '🩺', 8000, 5200, '2026-09-30', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Dr. Keisha B.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'The Nguyen Family', 'Jacksonville', 'FL',
       'Vietnamese immigrant family of four facing eviction after predatory landlord doubled their rent.',
       'Minh and Lan Nguyen immigrated to Jacksonville 8 years ago. They have two children in elementary school and Minh works as a mechanic while Lan is a home healthcare aide. Their landlord doubled the rent with 30 days notice — a tactic that is unfortunately legal in Florida. They are working with a housing attorney but need funds to secure a new apartment deposit and moving costs while the case is reviewed.',
       '🏡', 5000, 2100, '2026-07-15', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'The Nguyen Family');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Rosa & Miguel D.', 'Jacksonville', 'FL',
       'Elderly couple living on fixed income; facing rising grocery costs and transportation barriers.',
       'Rosa (78) and Miguel (81) Dominguez have lived in the same Northeast Jacksonville neighborhood for 40 years. Their combined Social Security income of $1,640/month barely covers rent and utilities. With no car and the nearest grocery store 3 miles away, they rely on a volunteer-driven meal program that recently reduced its service days. Your support pays for grocery delivery and supplemental meals.',
       '🌽', 600, 480, '2026-05-31', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Rosa & Miguel D.');

INSERT INTO uwnefl_profiles
    (name, city, state, blurb, story, emoji, goal, raised, deadline,
     identity_verified, needs_reviewed, paypal_confirmed, story_reviewed, status)
SELECT 'Sgt. Patricia H.', 'Jacksonville', 'FL',
       'Female Marine sergeant, single parent, navigating VA backlog for disability benefits.',
       'Patricia served 12 years as a Marine Corps logistics sergeant, including deployments to Iraq and Bahrain. She returned home with hearing loss and a spinal injury sustained in a vehicle accident during her service. Her VA disability claim was filed 14 months ago and remains in the backlog. In the meantime, she''s supporting her 9-year-old son on a part-time salary. Your donation helps cover rent, medication co-pays, and childcare while her claim is processed.',
       '🏅', 5500, 3900, '2026-10-31', 1, 1, 1, 1, 'published'
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_profiles WHERE name = 'Sgt. Patricia H.');

-- Category links
INSERT INTO uwnefl_profile_categories (profile_id, category_id)
SELECT p.id, c.id
FROM (SELECT 'Marcus T.' AS nm, 'Education' AS cat UNION ALL
      SELECT 'Diane & Ray K.', 'Health' UNION ALL
      SELECT 'Sophia L.', 'Housing' UNION ALL
      SELECT 'The Rivera Family', 'Food & Nutrition' UNION ALL
      SELECT 'James W.', 'Veterans' UNION ALL
      SELECT 'Amara O.', 'Education' UNION ALL
      SELECT 'Dr. Keisha B.', 'Health' UNION ALL
      SELECT 'The Nguyen Family', 'Housing' UNION ALL
      SELECT 'Rosa & Miguel D.', 'Food & Nutrition' UNION ALL
      SELECT 'Sgt. Patricia H.', 'Veterans') m
JOIN uwnefl_profiles p ON p.name = m.nm
JOIN uwnefl_categories c ON c.label = m.cat
WHERE NOT EXISTS (
    SELECT 1 FROM uwnefl_profile_categories pc WHERE pc.profile_id = p.id AND pc.category_id = c.id
);

-- Donors and donations from "This Week's Donors", only added while there are no donations yet.

INSERT INTO uwnefl_users (username, display_name, role)
SELECT v.username, v.display_name, 'USER'
FROM (
    SELECT 'cwashington' AS username, 'C. Washington' AS display_name UNION ALL
    SELECT 'rfitzgerald',             'R. Fitzgerald'                 UNION ALL
    SELECT 'okafor',                  'L. & T. Okafor'                UNION ALL
    SELECT 'jmartinez',               'J. Martinez'                   UNION ALL
    SELECT 'pchen',                   'P. Chen'                       UNION ALL
    SELECT 'kimfamily',               'The Kim Family'                UNION ALL
    SELECT 'gpatel',                  'G. Patel'                      UNION ALL
    SELECT 'mthompson',               'M. Thompson'
) v
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_donations)
  AND NOT EXISTS (SELECT 1 FROM uwnefl_users u WHERE u.username = v.username);

-- An hour apart, newest first. NULL username = anonymous.
INSERT INTO uwnefl_donations (profile_id, user_id, amount, donated_at)
SELECT p.id, u.id, d.amount, NOW() - INTERVAL d.hours_ago HOUR
FROM (
    SELECT 'Sgt. Patricia H.' AS nm, 'cwashington' AS username, 2500 AS amount, 1 AS hours_ago UNION ALL
    SELECT 'Diane & Ray K.',         NULL,                      1800,           2              UNION ALL
    SELECT 'Marcus T.',              'rfitzgerald',             1200,           3              UNION ALL
    SELECT 'Amara O.',               'okafor',                  900,            4              UNION ALL
    SELECT 'James W.',               NULL,                      750,            5              UNION ALL
    SELECT 'Sophia L.',              'jmartinez',               500,            6              UNION ALL
    SELECT 'The Rivera Family',      'pchen',                   400,            7              UNION ALL
    SELECT 'Dr. Keisha B.',          NULL,                      350,            8              UNION ALL
    SELECT 'The Nguyen Family',      'kimfamily',               300,            9              UNION ALL
    SELECT 'Rosa & Miguel D.',       'gpatel',                  250,            10             UNION ALL
    SELECT 'Marcus T.',              'mthompson',               200,            11             UNION ALL
    SELECT 'Sophia L.',              NULL,                      150,            12
) d
JOIN uwnefl_profiles p ON p.name = d.nm AND p.status = 'published'
LEFT JOIN uwnefl_users u ON u.username = d.username
WHERE NOT EXISTS (SELECT 1 FROM uwnefl_donations);
