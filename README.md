# C3S.ai Giving Platform

The United Way NEFL giving platform, built from the `reference/browse.html` and
`reference/admin.html` prototypes as a JSP front end over servlets and MySQL. One Maven project
that deploys as one WAR with three sides:

- **`/`** - the public giving page. Donors browse published recipient profiles, filter them by
  category, and see live totals and a weekly donor ticker. Signing in is optional.
- **`/admin`** - the staff dashboard: recipient profiles, categories and the fund allocation policy.
- **`/report`** - the financial report from `reference/report.html`, for administrators, with its
  own sign-in.

The profile-detail page, the donate flow and the Donors page are stubs in the prototypes and are
still stubs here.

## Stack

- **Front end** - JSP with plain Java scriptlets (no JSTL/EL), two stylesheets (`app.css`, and
  `report.css` for the report), one small vanilla JS file per feature. Fonts are Antonio + Palanquin from Google Fonts.
- **Backend** - Jakarta Servlet 6.1 / JSP 4.0 on Tomcat 10.1+, Java 21. Tomcat 9 will not run it.
- **Database** - MySQL 5.7+ through HikariCP, plain JDBC; the SQL avoids `WITH` and window
  functions. 8.0.16+ also enforces the `CHECK` that a donation's split adds up. Every table is
  prefixed `uwnefl_`.
- **Auth** - form sign-in against `uwnefl_users` with BCrypt cost 12, three roles, three session
  scopes.
- **Logging** - SLF4J + Logback; `MdcFilter` puts `requestId` and the signed-in user on every line.

A servlet builds a `*PageData` object, stores it under its class's simple name and forwards to a
JSP, which casts it back out. `FlashScope` carries a message across a redirect. `HtmlUtil` supplies
`esc`, `csrfInput` and `fieldError`, so the views never repeat that markup.

## Setup

**1. Schema.**

```bash
mysql --default-character-set=utf8mb4 -u <user> -p <db> < common/src/main/resources/db/schema.sql
```

Seven tables, six stock categories and the default allocation policy. Every environment, production
included, is set up clean from this file - there are no migrations. It drops every `uwnefl_` table
first, so running it against a live database wipes all its data. It creates **no accounts** -
insert an `ADMINISTRATOR` by hand before anyone can reach `/admin`:

```sql
INSERT INTO uwnefl_users (username, password_hash, display_name, role)
VALUES ('jane', '<bcrypt hash, cost 12>', 'J. Doe', 'ADMINISTRATOR');
```

For local development, `db/seed.sql` adds 10 recipients, 25 donations - 12 today, one in each of the
11 weeks before (the report's weekly chart) and two a year ago (its year-on-year figure) - and the
logins `admin`/`admin123`, `employee`/`employee123` and `user`/`user123`. It is excluded from the
WAR and must never run against production. Its donations are dated relative to when you seed, so the
ticker empties after a week; re-run `schema.sql` then `seed.sql` to refresh.

**2. Config.** Each key is read as `-Dkey`, then `$ENV_VAR`, then `db.properties` on the classpath
(`common.config.Config`). `db.properties` is git-ignored and excluded from the WAR;
`db.properties.example` is the template.

| Key | Env var | Default |
|---|---|---|
| `jdbc.url` / `jdbc.user` / `jdbc.password` | `JDBC_URL` / `JDBC_USER` / `JDBC_PASSWORD` | *required* |
| `jdbc.pool.maxSize` / `jdbc.pool.minIdle` | `JDBC_POOL_MAX_SIZE` / `JDBC_POOL_MIN_IDLE` | `10` / `2` |
| `browse.cache.seconds` | `BROWSE_CACHE_SECONDS` | `60` (`0` = off) |
| `upload.dir` | `UPLOAD_DIR` | `<user.home>/c3s-admin-uploads` |
| `session.cookie.secure` | `SESSION_COOKIE_SECURE` | `true` |
| `log.dir` / `log.level` | - | `<catalina.base>/logs` / `INFO` |

`log.dir` and `log.level` are read by Logback, so pass those as `-D` flags only.

**3. Build.**

```bash
CATALINA_HOME=~/.sdkman/candidates/tomcat/10.1.41 ./mvnw clean verify
```

Builds every module, runs the tests and produces `web/target/ROOT.war`. `verify` also precompiles
every JSP with Jasper (`build/verify-jsps.sh`) and runs `build/verify-no-markup-in-js.sh` and
`build/verify-report-css.sh`. Jasper comes from a Tomcat install, so set `CATALINA_HOME`; without
one the JSP check is skipped. Jasper compiles at an older source level than Java 21, so scriptlets
avoid newer syntax such as `instanceof` patterns.

**4. Deploy.** Copy `ROOT.war` into `webapps/`. Every link is built from `getContextPath()`, so it
also works under another context path. In production use `sslMode=REQUIRED`,
`SESSION_COOKIE_SECURE=true`, a database user limited to `SELECT`/`INSERT`/`UPDATE`/`DELETE`,
`RemoteIpValve` behind a proxy so the login throttle sees real client addresses, and an
`upload.dir` outside the Tomcat directory.

Pass the JDBC URL through the environment rather than `-D` in `JAVA_OPTS`: `catalina.sh` runs
`$JAVA_OPTS` through `eval`, and an unescaped `&` between URL parameters breaks it.

IntelliJ: open the root `pom.xml` and use the bundled run configuration
`donation (Tomcat 10.1.41)`. It deploys `donation-web:war exploded` at context path `/` - IntelliJ
resets that to `/donation_web_war_exploded` if the artifact is re-added.

## Roles and sign-in

| Route | `ADMINISTRATOR` | `EMPLOYEE` | `USER` |
|---|:---:|:---:|:---:|
| `/`, `/login`, `/logout` | yes | yes | yes |
| `/admin`, `/admin/profiles`, `/admin/profile`, `/admin/profiles/archive`, `/photos/*` | yes | yes | - |
| `/admin/categories`, `/admin/category`, `/admin/allocations`, `/admin/allocation`, the archive posts | yes | - | - |
| `/report`, `/report/login`, `/report/logout` (own sign-in) | yes | - | - |

The rules are an allow-list at the top of `core.servlet.AuthFilter`, so a newly added `/admin` path
is `ADMINISTRATOR`-only until it is listed. An unrecognised role value reads as `USER`. A signed-out
GET of an admin page goes to `/admin/login?next=<page>` and lands back on that page after sign-in;
`Routes.adminReturnPath` only accepts plain `/admin/...` paths, so `next` can't send anyone
off-site.

**Three sign-in doors, one implementation.** Donors sign in at `/login`, staff at `/admin/login`,
and administrators at `/report/login` for the report. The last two share one page
(`admin/login.jsp`), which posts back to whichever door served it. All are
`AbstractLoginServlet`, which holds the only copy of the credential check, the throttles and the
session handling; a subclass supplies its JSP, its scope, the roles it accepts and where it lands.

**Three session scopes.** One `JSESSIONID` holds `userAuth` (30 min idle), `adminAuth` and
`reportAuth` (15 min idle each) independently. `/login` never opens an admin door, even for an
`ADMINISTRATOR`, who signs in again at `/admin/login`; a `USER` with the right password there gets
a 403 and no scope. The dashboard and the report don't open each other either, so signing in or out
of one leaves the other as it was. All three can be live at once as different people. Each scope's
idle clock moves only with its own side's traffic, and a scope ends 8 hours after sign-in
regardless. Signing in rotates the session id and the CSRF token.

**Credentials.** `core.user.AuthService` is the only code that can read a password hash - the hash,
its record and its DAO are package-private. A sign-in returns a sealed `LoginResult` of
`Success` / `Rejected` / `Throttled` carrying an `AuthenticatedUser` with no hash. An unknown
username, a donor row with no password and a corrupt hash are all checked against a dummy BCrypt
hash, so neither the timing nor the message gives them away.

Two in-memory throttles lock for 15 minutes: 5 attempts per account, 20 per client address. An
attempt is counted before the password is checked, in one atomic step, so parallel requests cannot
slip past the limit while BCrypt runs; and the account bucket is keyed on the username the database
matched, folded the way MySQL's collation compares it, so `admin`, `ADMIN` and `ádmin` share one
bucket. There is no sign-up or password reset.

## Data model

```
uwnefl_categories  1 -< uwnefl_profile_categories >- 1  uwnefl_profiles
uwnefl_categories  1 -< uwnefl_allocation_settings (category_id NULL = the default policy)
uwnefl_allocation_settings  1 -< uwnefl_allocation_buckets
uwnefl_profiles    1 -< uwnefl_donations >- 0..1  uwnefl_users
```

- **`uwnefl_users`** - staff and donors in one table; a NULL `password_hash` is a donor who cannot
  sign in.
- **`uwnefl_profiles`** - `status` is `draft` / `published` / `archived`; archiving is a soft
  delete. A `CHECK` keeps `raised <= goal`. Four review flags are set in the admin form.
- **`uwnefl_categories`** - `color_class` is one of six values, enforced by `CategoryValidator` and
  a DB `CHECK`. Archiving keeps existing links but drops the category from the profile form's
  picker, the giving page's chips and the cards.
- **`uwnefl_profile_categories`** - many-to-many; a profile cascades, a category in use is
  `RESTRICT`.
- **`uwnefl_allocation_settings`** / **`_buckets`** - one default policy (`id = 1`, `category_id`
  NULL) plus an optional per-category override. A policy has a fee model
  (`deducted` / `added` / `absorbed`), a processor rate, a fixed fee and percentage buckets.
- **`uwnefl_donations`** - whole dollars; `user_id` NULL means anonymous; `frequency` is
  `one_time` / `monthly`. Each row carries the allocation split it settled under - the policy
  (`settings_id`, fee model, rates, unrestricted %) and the charge, fee, processing, restricted and
  unrestricted amounts - so the report never moves when a policy is edited. The formulas are in
  `schema.sql`, and a `CHECK` rejects a split that doesn't add up to the charge. Whatever
  eventually records donations (the donate flow is still a stub) must fill them.

## Routes

| Route | Method | Notes |
|---|---|---|
| `/` | GET | the giving page, via the `index.jsp` welcome file |
| `/login`, `/admin/login`, `/report/login` | GET, POST | 401 wrong credentials, 403 wrong door, 429 throttled |
| `/logout`, `/admin/logout`, `/report/logout` | POST | ends that scope only |
| `/admin/profiles` | GET | every profile, any status |
| `/admin/profile[?id=]` | GET, POST | add/edit form; the POST is multipart |
| `/admin/profiles/archive` | POST | soft delete |
| `/admin/categories`, `/admin/category[?id=]`, `/admin/categories/archive` | GET, POST | the same shape for categories |
| `/admin/allocations`, `/admin/allocation[?categoryId=]` | GET, POST | the default policy and per-category overrides |
| `/report` | GET | the financial report: year to date, this month's top donors, the last 12 weeks |
| `/photos/<uuid>.<ext>` | GET | stored photos; any other name is a 404 without touching disk |
| `/healthz` | GET | `SELECT 1` on its own small pool, capped at 4s; 200 or 503, no auth |

An unknown URL redirects to the home page of the side it was aimed at. 403, 405, 500 and any
uncaught exception render one error page, `views/common/error.jsp`, which shows staff the status
code and a link back to the dashboard, and everyone else plain wording and a link back to giving -
so a donor who pokes at an `/admin` URL gets the donor version. Stack traces are logged, not
rendered.

## Validation

Every rule lives in the service that owns the table - `ProfileService.validate`,
`CategoryService.validate`, `AllocationService.prepare` - so a servlet cannot skip it. A failure
re-renders the form with 422, the submitted values and a message per field.

| Profile field | Rule |
|---|---|
| Name | required, max 100 |
| Blurb | required, max 160 |
| Story | optional, max 2000 |
| City / State / Emoji | max 100 / 2 letters / max 8 |
| Categories | at least one, each still active |
| Goal | required, whole number, at least $100 |
| Already raised | required, whole number, 0 up to the goal |
| Deadline | optional, not in the past |

Lengths match the column widths in `schema.sql`, so an over-long value is a field error rather than
a database error. A category label is required, max 100, unique, with a colour from the fixed list;
a duplicate that slips past the check comes back as a field error, not a 500. An allocation policy
must total exactly 100% across at most 6 named buckets, with a processor rate of 0-10% and a fixed
fee of $0-$5.

## Security

**Who can read what.** Each table's DAO is package-private inside that table's package, so the only
way to its data is the service or query facade that owns it:

- `core.user` holds credentials; only `AuthService` can read a hash.
- `admin.*` holds the dashboard's data, and `browse` does not depend on `admin`, so giving-site code
  cannot even name those classes.
- `report.*` reads the same way as `browse`: `DonationQueries` and `PolicyQueries` over
  package-private DAOs extending `JdbcReader`, and it depends on neither `browse` nor `admin`.
- `browse.*` exposes one public facade per table (`ProfileQueries`, `CategoryQueries`,
  `DonationQueries`) over a package-private DAO extending `JdbcReader`, which has no write helpers.
- `DBUtil.getConnection()` is package-private, so nothing outside the two JDBC base classes can open
  a raw connection.

`DataIsolationTest` in `browse`, `admin` and `report` asserts all of this against the real test
classpath.

**Everything else.** All SQL is parameterised, all output goes through `HtmlUtil.esc`, and user
input is cleaned with `Log.clean` before it is logged. Every non-GET needs a CSRF token, checked by
`AuthFilter` - except the multipart `POST /admin/profile`, which `ProfileServlet` checks once the
parts are parsed. A stale token saves nothing and signs nobody out. Every `/admin` response is
`no-store`. `SecurityHeadersFilter` sends `nosniff`, `X-Frame-Options: DENY`, a referrer policy, a
CSP whose `script-src` is `'self'`, and HSTS when the deployment is HTTPS; filter order is declared
in `web.xml`. Archived rows are read-only in the service *and* in the UPDATE statement. Nothing
structural is taken from a form: row ids, allocation bucket structure and pending photo names are
all resolved or checked server-side. An upload must decode as the format its extension claims,
within 4000px a side and 12 megapixels, and at most two decode at once.

**Caching.** The giving page is built at most once per `browse.cache.seconds` and shared
(`BrowsePageCache`); the dashboard sidebar's counts are held until a write (`SidebarStatsCache`).
Any dashboard write clears both through `AppCaches`, so an archived profile leaves the giving page
at once, and a load already running when the write lands is discarded rather than stored. Caches
are per JVM, so a change made straight in the database still waits for the TTL.

## Styling and scripts

`css/app.css` styles everything but the report, in three blocks: shared tokens and chrome, the
giving site, the dashboard. The two halves share no component classes. One JS file per feature under `js/`, loaded
per page through `views/common/head.jspf`; `core.js` defines the one global, `window.C3S`, and
every other file registers with it and does nothing on a page without its markup.

**The markup is the JSP's, always.** No script creates an element or assigns HTML. Where a page
changes as you use it, the JSP renders every state it can reach and hides the rest - all six
allocation bucket rows, a preview slot per bucket, a preview tag per category - and the script
flips `hidden` and sets `textContent`. So the page is right before any script runs, and every value
is escaped once, in the JSP. `build/verify-no-markup-in-js.sh` fails the build if that slips.

**The report keeps the prototype's stylesheet.** `css/report.css` is the `<style>` block of
`reference/report.html`, byte for byte, and `/report` loads it instead of `app.css`.
`build/verify-report-css.sh` fails the build if the two differ, so restyle the reference and copy it
across. It shares everything else with the other pages: `common/head.jspf` (each page names its
stylesheet), `progress-bars.js` for bar widths, and `fee-split.js`, the fee-model split that the
allocation form's preview uses too. Its what-if controls (`js/report.js`) re-split the rendered
figures in the browser and save nothing; Export PDF is the browser's print dialog. The
unrestricted-share slider runs to 50% as in the reference, or further when the default policy's
share is already higher.

Bump `Assets.VERSION` when a file under `/css` or `/js` changes; it is the `?v=` on every link.

## Tests

```bash
./mvnw test                  # every module
./mvnw -pl core -am test     # one module and what it depends on
```

Tests live in the module whose code they cover: the utilities and the JDBC base in `common`;
sign-in, session, throttle and filter tests in `core`; `BrowseServiceTest`, `BrowseStyleTest` and
`DataIsolationTest` in `browse`; the service rules, uploads and the donor message in `admin`;
`ReportServiceTest`, `ReportFormatTest`, `ReportStyleTest` and `DataIsolationTest` in `report`. Each
table also has one `*DaoIT` covering its queries.

The `*IT` tests need a database and skip themselves without one. They create a temporary
`uwnefl_it_<hex>`, load `schema.sql` + `seed.sql` and drop it when the JVM exits, so your own
databases are untouched:

```bash
DONATION_IT_JDBC_URL='jdbc:mysql://localhost:3306/?sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8' \
DONATION_IT_JDBC_USER=root DONATION_IT_JDBC_PASSWORD=... ./mvnw test
```

## Modules

Each module depends only on the ones above it; the root `pom.xml` pins every version.

```
donation/                      parent pom - versions, compiler settings, module list
  common/  donation-common jar  config, exceptions, utilities, the HikariCP pool and the
                                JdbcReader / JdbcWriter base classes; db/schema.sql + seed.sql
  core/    donation-core   jar  core.user (sign-in), core.auth (session, CSRF, throttle),
                                core.web (page data, helpers, caches), core.servlet (filters,
                                base servlets, error pages)
  browse/  donation-browse jar  browse.profile / .category / .donation - one public query facade
                                per table; browse.page - the giving page and donor sign-in
  admin/   donation-admin  jar  admin.profile / .category / .allocation - model, DAO, service,
                                validator, servlets and page data per table; admin.shell - the
                                dashboard chrome, staff sign-in, photos and caches
  report/  donation-report jar  report.donation / .policy - one read-only query facade per table;
                                report.page - the financial report's service, servlet and view
  web/     donation-web    war  JSPs, css/*.css, js/*.js, web.xml; packages ROOT.war
  build/                        verify-jsps.sh, verify-no-markup-in-js.sh, verify-report-css.sh
  reference/                    the HTML prototypes (documentation, not packaged)
```

**One folder per database table.** A table's read models, its package-private DAO, the service that
owns its rules, its validator, its servlets and its page data sit together, and every folder is
built the same way: a public service (dashboard) or query facade (giving site) in front of a
package-private `<Table>Dao` and `Jdbc<Table>Dao`, with a public no-arg constructor that wires the
real DAO and a package-private one for tests.

`common` has no servlet API and no knowledge of the app's tables; `core` is the web plumbing plus
sign-in; `browse` and `report` read; `admin` reads and writes. None of the three depends on another.

## Known gaps

Deliberately unchanged - these need a decision about what the numbers should mean, not a code fix:

- `uwnefl_profiles.raised` is typed in by staff and never updated by donations, so the campaign
  totals and the donation-based figures are two different things and are not reconciled.
- "Total raised this year" and "Active donors" are all-time sums; `raised` has no date.
- "Raised by category" counts a profile once per category, so the bars can exceed the total.
  The report's "Amount Raised by Category" does the same, so its shares can add past 100%.
- The report's what-if controls re-split every recipient's gifts under one policy, but the figures
  the page loads with are each donation's own snapshot, which can come from several policies. The
  controls start from the default policy, so the first move can shift a figure that looked settled.
- Seeded deadlines are in the past, so editing a seeded profile fails validation until you change
  its deadline.
