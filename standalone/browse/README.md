# C3S.ai Browse

The donor-facing "Give / Browse" page from `reference/browse.html`, rebuilt as a
JSP front end over servlets and MySQL (plain JDBC). Same stack and conventions
as the `../admin` module.

The page itself is public and **read-only**: it shows published recipient
profiles and a few aggregates. Users can also sign in with a **browse
account**. That's a separate set of users from the admin app's admins, with
its own sign-in page. Browse users are also the donors: a donation points at
the user who gave it, and the names in "This Week's Donors" come from that
user. A donation with no user is anonymous.

The profile-detail page, the donate flow and the Report page are stubs in the
prototype and still are here: "View profile & donate" and "Report" render but
do nothing.

## Stack

- **Front end**: JSPs with plain Java scriptlets (`<% %>` / `<%= %>`):
  `browse.jsp`, `login.jsp` and `error.jsp` under `WEB-INF/views/`.
  Everything from the DB or the request is escaped through `HtmlUtil.esc`.
  - `webapp/css/browse.css` is an exact copy of the prototype's `<style>`
    block, with nothing added. Like the prototype, the page sets category
    colours, card backgrounds and bar widths with inline `style` attributes.
    The sign-in and error pages keep their own styles in `login.css` and
    `error.css`.
  - `webapp/js/browse.js` does the category filter, sort and search over the
    server-rendered cards. It only shows, hides and reorders them; it never
    builds markup.
  - `webapp/js/login.js` adds the show/hide password toggle on the sign-in
    page. The button stays hidden if JavaScript doesn't run.
  - Fonts (Antonio + Palanquin) come from Google Fonts, same `<link>` as the
    prototype.
- **Backend**: Jakarta Servlets 6.1, annotation-mapped, built for Tomcat 11.
  Also runs on Tomcat 10.1, since nothing uses a 6.1-only API.
- **DB**: MySQL over JDBC (`mysql-connector-j`), pooled with HikariCP. It
  reads **the same database the admin app writes**; every table is prefixed
  `uwnefl_`. Use **MySQL 8.0+ in production**. 5.7 is end-of-life and not
  supported by the driver version used here (it also ignores `CHECK`
  constraints), so keep it for local dev only.
- **Auth**: form login against `uwnefl_browse_users`, with BCrypt password hashes.
- **Logging**: SLF4J + Logback to stdout and a rolling file. Each line carries
  `requestId`, plus `user` once someone is signed in (`MdcFilter`).

### How a request renders

`GET /` → `BrowseServlet` → `BrowsePageCache` → `BrowseService.buildPage()`
builds a `BrowsePageData` (cards, the 5 stats, category strip, ticker) from
three DAOs → put on the request as `"BrowsePageData"` → forwarded to
`browse.jsp`.

The page data is the same for every visitor, so it's cached app-wide for
`browse.cache.seconds` (60 by default). Who's signed in is read from the
session in the JSP, not from the cached data. The sign-in page works the same
way with `LoginPageData`, without the cache.

## Data model

The recipient tables (`uwnefl_profiles`, `uwnefl_categories`,
`uwnefl_profile_categories`) belong to the admin app. `browse-schema.sql` has a
copy of them so browse can be set up on its own. Browse adds two tables of its
own:

```
uwnefl_browse_users  0..1 ──< *  uwnefl_donations  * >── 1  uwnefl_profiles (admin)
 (username, password,             (amount, donated_at)         (the recipient)
  display_name)
```

**`uwnefl_browse_users`**: `username`, `password_hash`, `display_name`,
`created_at`.
- `display_name` is the public name, e.g. "C. Washington" or "The Kim Family".
- `password_hash` is optional. A row without one is a donor record that can't
  sign in.

**`uwnefl_donations`**: `profile_id` (who received it), `user_id` (who gave
it), `amount` in whole dollars, `donated_at`.
- **`user_id` NULL means anonymous.** There's no separate flag.
- Deleting a recipient profile deletes its donations.
- Deleting a user keeps their donations, but they become anonymous
  (FK `ON DELETE SET NULL`). From then on, each of those donations counts as a
  separate donor in the stats.

### Where the numbers come from

| Page element | Source |
|---|---|
| Cards, "Lives supported", "Total raised this year", "Raised by category" | `uwnefl_profiles` (status `published`) + `uwnefl_profile_categories` + `uwnefl_categories` |
| "This Week's Donors" ticker | donations from the last 7 days, newest first, at most 12, with the donor's `display_name`, or "Anonymous" when there's no user. The bar is hidden when there are none. |
| "Active donors" | distinct donors with a donation to a published profile |
| "New donors this month" | donors whose first donation was this calendar month |
| "Raised this week" | sum of donations from the last 7 days |
| Card badge "Top this week" + "+$X raised this week" | the 5 published profiles with the most donations in the last 7 days; +$X is that 7-day total. On a tie, the older profile wins. Other cards show neither. |

A few things worth knowing:

- `uwnefl_profiles.raised` (what admins type in) is the campaign total. The
  donations table only drives the time-based and donor figures. The two are
  **not** reconciled.
- "Total raised this year" is the sum of `raised` over published profiles.
  There's no date on that column, so it isn't really limited to this year.
- For the donor stats, a donor is their user. An anonymous donation can't be
  tied to anyone, so each one counts as a donor of its own.
- "Last 7 days" and "this calendar month" are measured in **UTC**. Every DB
  connection sets `time_zone = '+00:00'`, so the result doesn't depend on the
  DB server's time zone.
- Everything on the page can be up to `browse.cache.seconds` old.
- A card's colour and tag come from its first category, in the order admin
  created the categories.
- The category chips and "Raised by category" list only active categories that
  have a published profile, in the order admin created them. An unused "Other"
  doesn't appear. Their labels are the category names from admin, so it's
  "Food & Nutrition" where the prototype shortened it to "Food".
- Cards are listed oldest first ("Featured"), which keeps the demo profiles in
  `browse.html`'s order.
- Like the prototype, the stats bar and category strip shorten amounts ($21.8K,
  $9.3K); cards and the ticker show them in full.
- Like the prototype, picking a category resets the sort to "Featured" and
  clears the search, typing a search resets the sort, and changing the sort
  clears the search.

## Sign in (users, not admins)

The code follows the admin app's sign-in pattern, but it's a different set of
accounts and a separate process:

| | Admin app | Browse |
|---|---|---|
| Who | admins | users (donors) |
| Accounts table | `uwnefl_users` | `uwnefl_browse_users` |
| Sign-in page | admin's split-screen login | `/login` here, also split-screen but themed for giving |
| After sign-in | admin dashboard | back to the browse page |
| Demo account | `admin` / `admin123` (seeded by admin's `schema.sql`) | `user` / `user123` (only in `browse-seed.sql`) |

- **The two can't sign in to each other.** Browse only ever queries
  `uwnefl_browse_users` and admin only ever queries `uwnefl_users`. They're also
  different WARs, so the sessions are separate too.
- **Sign in** in the nav opens `/login`. The left side is a brand panel with
  the United Way logo, a headline and three trust points; on phones it shrinks
  to a logo bar. The right side has the form, with a show/hide password toggle,
  a **Cancel** button under **Sign in** that goes back to the browse page,
  and a link for admins who landed on the wrong sign-in page. After a failed
  attempt the username stays filled in and the cursor starts in the password
  field.
- On success you're sent back to the browse page, and the nav shows
  "Signed in as *display name*" with a **Sign out** button.
- Passwords are checked with BCrypt (`AuthService`). An unknown username, or a
  donor record with no password, is checked against a dummy hash. That way the
  response time doesn't reveal which usernames exist, and the error message is
  the same in every case.
- On success the old session is thrown away and a new one is started
  (`AuthSession.begin`), which prevents session fixation.
- **Two throttles**, both in memory and per JVM (`LoginThrottle`), each giving
  a 429 for 15 minutes once it trips:
  - **per username:** 5 failed attempts inside 15 minutes lock that username
  - **per client address:** 20 failed attempts inside 15 minutes lock that
    address, so one machine can't try a password across many usernames
  - Behind a reverse proxy, configure Tomcat's `RemoteIpValve`. Otherwise every
    visitor shares the proxy's address and the per-client limit locks them all
    out together.
- Sign in and sign out are both POSTs carrying a per-session CSRF token
  (`Csrf`). There's no auth filter because every page is public, so
  `LoginServlet` and `LogoutServlet` check the token themselves.
- Anonymous visitors never get a session. One is only created when someone
  opens the sign-in page.
- Session cookie: `HttpOnly`, `SameSite=Lax`, 30-minute timeout, plus `Secure`
  when `session.cookie.secure=true`. It's Lax rather than admin's Strict
  because people reach this public page from links. With Strict, a signed-in
  user would look signed out on that first page load.

There's no sign-up or password reset yet. To add a user, insert a row with a
BCrypt hash (cost 12):

```sql
INSERT INTO uwnefl_browse_users (username, password_hash, display_name)
VALUES ('jane', '<bcrypt hash>', 'J. Doe');
```

## Setup

1. **Schema.** `browse-schema.sql` creates every table browse reads:
   admin's `uwnefl_categories`, `uwnefl_profiles` and
   `uwnefl_profile_categories` (copied from admin's `schema.sql`), plus
   browse's own two. It uses `CREATE TABLE IF NOT EXISTS` throughout, so it
   doesn't matter whether admin's script or this one runs first, and running
   it again changes nothing. It inserts **no rows**, so it's safe for
   production.

   ```
   mysql --default-character-set=utf8mb4 -h <host> -u <user> -p <db> < src/main/resources/db/browse-schema.sql
   ```

   If admin's `schema.sql` changes one of those three tables, update the copy
   here too.

   **Seed (local demo only, never on a real database).** Load the demo content
   taken from `browse.html` and `admin.html`:

   ```
   mysql --default-character-set=utf8mb4 -h <host> -u <user> -p <db> < src/main/resources/db/browse-seed.sql
   ```

   It adds:
   - the `user` / `user123` sign-in account
   - admin's 6 categories (the same rows admin's `schema.sql` adds)
   - the 10 prototype recipients (published) and their category links
   - the 8 named donors from "This Week's Donors" (no password)
   - its 12 donations, 4 of them anonymous, dated an hour apart over the last
     12 hours in the prototype's order

   With that data the badges go to Patricia, Diane & Ray, Marcus, Amara and
   James. Every figure is computed, so the page doesn't show the prototype's
   hardcoded figures. Running it again is safe: existing rows are skipped, and
   donations are only added while there are none. Keep
   `--default-character-set=utf8mb4`, otherwise the emoji get mangled.

   **Demo dates go stale.** The demo donations are dated relative to when the
   seed ran, so after a week "This Week's Donors" and the weekly badges go
   empty. To refresh a local database, clear the donations and run the seed
   again:

   ```sql
   DELETE FROM uwnefl_donations;
   ```

   `browse-seed.sql` is left out of the WAR.

2. **Connection config.** Works the same as admin. Each key is looked up as
   `-Dkey`, then the env var, then `src/main/resources/db.properties`.
   `db.properties` is for local dev only: it's `.gitignored` and left out of the
   WAR. Copy `db.properties.example` to start.

   | Key | Env var | Default |
   |-----|---------|---------|
   | `jdbc.url` | `JDBC_URL` | *(required)* |
   | `jdbc.user` | `JDBC_USER` | *(required)* |
   | `jdbc.password` | `JDBC_PASSWORD` | *(required)* |
   | `jdbc.pool.maxSize` | `JDBC_POOL_MAX_SIZE` | `10` |
   | `jdbc.pool.minIdle` | `JDBC_POOL_MIN_IDLE` | `2` |
   | `browse.cache.seconds` | `BROWSE_CACHE_SECONDS` | `60` (`0` = no cache) |
   | `session.cookie.secure` | `SESSION_COOKIE_SECURE` | `false` |
   | `log.dir` | `LOG_DIR` | `<catalina.base>/logs` |
   | `log.level` | - | `INFO` |

3. **Build:**

   ```
   ./mvnw clean package
   ```

   This runs the unit tests and produces `target/browse.war`. With
   `CATALINA_HOME` pointing at a Tomcat 10.1/11 install, `./mvnw verify` also
   precompiles the JSPs with Jasper (`build/verify-jsps.sh`), so a broken
   scriptlet fails the build. Without it, that step is skipped with a notice.

4. **Deploy:** drop `target/browse.war` into `webapps/`. It's served at
   `/browse/`, or at `/` if you deploy it as `ROOT.war`.

   "+ Admin" in the nav links to `/admin`, so it expects the admin WAR on the
   same host.

   **Running from IntelliJ:** its Tomcat serves `target/browse` but doesn't
   pick up changed classes on its own. After `./mvnw package`, restart the
   server or use *Update classes and resources*. Otherwise the page keeps
   running the old code.

   If the DB is down at startup, the app still deploys. The startup check logs
   a clear error, `/healthz` reports `DOWN`, and the pool connects once the DB
   is back.

### Production checklist

- MySQL 8.0+.
- Only `browse-schema.sql` has been run, **never `browse-seed.sql`**, so there are no
  demo profiles, donations or `user` account. Also change or remove admin's
  seeded `admin` account.
- Credentials come from env vars or `-D` flags, not `db.properties`. The URL has
  `sslMode=REQUIRED`, and the app's DB user has `SELECT` only (the app never
  writes). Schema scripts run with a separate, more privileged user.
- HTTPS in front, with `SESSION_COOKIE_SECURE=true`.
- Behind a proxy or load balancer, Tomcat's `RemoteIpValve` is configured, so
  the per-client login throttle sees real client addresses. The throttles are
  per JVM, so also rate-limit `POST /login` at the proxy.
- Tomcat's `ErrorReportValve` has `showServerInfo="false"` and
  `showReport="false"`, for any error the app's own pages don't cover.
- `BROWSE_CACHE_SECONDS` suits how fresh the numbers need to be.

## Pages

- `GET /` - the browse page. Sends `Cache-Control: private, no-cache` when the
  visitor has a session, because the page then carries their name and a CSRF
  token.
- `GET /login` - the sign-in page (`Cache-Control: no-store`). If you're already
  signed in, it redirects to `/`.
- `POST /login` (`username`, `password`, `_csrf`) - success: 302 to `/`.
  Bad credentials: the page again with 401. Locked out: 429. Missing or stale
  token: 302 back to a fresh page.
- `POST /logout` (`_csrf`) - ends the session, 302 to `/`. `GET /logout` just
  redirects to `/`.
- `GET /healthz` - runs `SELECT 1` with a 4s cap. Returns
  `200 {"status":"UP","db":"UP"}`, or `503` if the DB can't be reached.
  `Cache-Control: no-store`.
- Any unknown URL gets a 302 back to `/` (`NotFoundServlet`). A method a page
  doesn't support (e.g. `POST /`) gets the app's error page with 405.

## Security

- `SecurityHeadersFilter` sets a CSP on every response: `'self'` plus Google
  Fonts, and no inline scripts. Inline `style` attributes are allowed, because
  the page uses them the same way the prototype does. It also sends `nosniff`,
  `X-Frame-Options: DENY` and `Referrer-Policy`. It adds HSTS on HTTPS
  requests.
- Pages that carry a session's data are marked so shared caches don't keep
  them (see [Pages](#pages)).
- All SQL uses fixed statements with bound parameters.
- The JDBC URL is logged with the credentials and query string stripped.
- Request-derived values are cleaned before they're logged (`Log.clean`), and
  inbound `X-Request-Id` headers are sanitised.
- The only thing shown publicly about a donor is their `display_name`.
  Anonymous donations aren't linked to any user at all.
- Sign-in protections are listed under [Sign in](#sign-in-users-not-admins).

## Errors

When something fails, it's logged with the full stack trace and the browser
only sees the generic `error.jsp`. The servlets' own catch blocks
(`PageServlet.showServerError`, HTTP 500) and the `<error-page>` mappings in
`web.xml` (405, 500, any exception) both send you there, so a raw Tomcat stack
trace never reaches a visitor.

## Tests

`./mvnw test` runs plain JUnit 5:

- `BrowseServiceTest` feeds fake DAOs into `BrowseService`. It checks the card
  percent and style, that it asks for the top 5 and only badges those, bar
  scaling in the category strip, and that stats, chips and ticker are passed
  through.
- `BrowsePageCacheTest` covers: the page is reused until the TTL runs out, a
  TTL of 0 rebuilds every time, and a failed build isn't cached.
- `BrowseStyleTest` checks the admin `color_class` → prototype CSS class
  mapping, including the fallback for unknown values.
- `HtmlUtilTest` checks the short amounts ($842K, $12.4K, $1M, $1.3M, and
  values under $1,000 left as they are) and HTML escaping.
- `AuthServiceTest` covers:
  - the right password returns the user with their display name
  - wrong password, unknown user and missing input are all rejected
  - the username is trimmed
  - a donor record with no password can't sign in
  - a corrupt stored hash is rejected without crashing
- `LoginThrottleTest` covers:
  - the lock kicks in exactly at the limit
  - keys ignore case and spaces
  - reset clears a lock, and locks are separate per key
  - the shared throttles lock after 5 (username) and 20 (client) misses
- `DaoIntegrationTest` runs the real SQL (`browse-schema.sql` and
  `browse-seed.sql`) in a throwaway MySQL schema and
  checks every DAO against it. It's skipped unless a server is configured, and
  drops the schema afterwards:

  ```
  BROWSE_IT_JDBC_URL='jdbc:mysql://localhost:3306/?sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8' \
  BROWSE_IT_JDBC_USER=root BROWSE_IT_JDBC_PASSWORD=... ./mvnw test
  ```

  The URL has no database name, and the user needs rights to create and drop
  a database.

The servlets and JSPs have no automated tests. After a deploy, run this manual
smoke check:

1. **Health:** `GET /healthz` returns 200.
2. **Browse page:** `GET /` shows the page; with no session there's no
   `Set-Cookie`.
3. **Sign-in page:** `GET /login` shows the form with `Cache-Control: no-store`.
4. **Wrong password:** returns 401 with the error message.
5. **Good sign-in:** 302 to `/`, the nav shows "Signed in as …", and `/`
   now sends `Cache-Control: private, no-cache`.
6. **Sign out:** returns to `/` and shows "Sign in".
7. **Wrong method:** `POST /` returns the app's error page with 405.

## Layout

```
servlet/    - HTTP entry: browse, login/logout, health check, security headers, MDC, startup/shutdown, PageServlet (shared rendering)
auth/       - AuthService (BCrypt), AuthSession, LoginThrottle
service/    - BrowseService (builds the page from the DAOs), BrowsePageCache
dao/        - JDBC, an interface + Jdbc* impl each: browse profiles, donations, categories, browse users
model/      - entities, view records (ProfileCard, CategoryTotal, ...), BrowseStyle
web/        - PageData + per-page data classes, Csrf
util/       - Config, HtmlUtil, Log (trimmed copies of admin's)
db/         - DBUtil, the HikariCP pool
exception/  - ConfigurationException, DataAccessException

resources/db/
  browse-schema.sql   - all tables browse uses (admin's three + browse's two), no rows
  browse-seed.sql     - demo data from browse.html / admin.html, local only, left out of the WAR
```
