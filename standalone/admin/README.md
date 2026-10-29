# C3S.ai Admin

The admin dashboard from `reference/admin.html` - recipient profiles,
admin-managed categories, and the fund allocation policy - rebuilt as a JSP
front end over servlets and MySQL (plain JDBC). A profile can belong to several
categories; the allocation policy has one default plus an optional override per
category.

## Stack

- **Front end**: JSP with plain scriptlets (`<% %>` / `<%= %>`).
  Every page pulls in `WEB-INF/views/common/header.jspf` and `footer.jspf` for
  the shared shell. Output is escaped through `HtmlUtil`.
- **Backend**: Jakarta Servlets 6.1, annotation-mapped, targeting Tomcat 11.
- **DB**: MySQL over JDBC (`mysql-connector-j`), pooled with HikariCP. Every
  table is prefixed `uwnefl_`.
- **Auth**: form login against `uwnefl_users`, BCrypt password hashes.
- **Logging**: SLF4J + Logback, to stdout and a rolling file. Each line carries
  `requestId` and `user` from the MDC (set by `MdcFilter`).

### How a request renders

Each page has its own data class - `LoginPageData`, `ProfileListPageData`,
`CategoryFormPageData`, ... - all extending `PageData`. The servlet stashes it
under the class's own name and the JSP reads it straight off the request:

```java
ProfileFormPageData data =
    (ProfileFormPageData) request.getAttribute("ProfileFormPageData");
```

`PageData` carries the one field every page needs, `message` (the flash /
notice / error line, never null); the form classes add a per-field error map
(`FormPageData`). `AdminServlet` does the plumbing: `render` (200), `renderRejected`
(401 / 422 / 429), `renderError` (403 / 404 / 500, to `error.jsp`) - each sets the
status and forwards.

Success messages travel as a session flash (`FlashScope.push` / `pull`), not on
the redirect URL. Page chrome - context path, current user, CSRF token, title,
breadcrumb, active nav - is worked out in the JSP, not carried in the data class.

### CSRF

Each session gets a token (`Csrf.tokenFor` / `Csrf.matches`, constant-time
compare). Every non-GET form submits it as a hidden `_csrf` field. `AuthFilter`
rejects any non-GET request whose token is missing or wrong, except the multipart
`POST /profile` - `AuthFilter` can't read a body field before the parts are
parsed, so `ProfileServlet` checks that one itself (403 on a mismatch).

### Sidebar stats

The "active profiles" / "raised total" counters come from `SidebarStatsCache`, an
app-scoped cache over one aggregate query. It's loaded lazily and
`invalidate()`d whenever a profile is saved or archived (`ProfileService` does
that) - not recomputed on every page load. Single JVM, so a direct DB edit
won't show until the next app-side change.

## Setup

1. **Schema.** One script, no migrations:

   ```
   mysql -h <host> -u <user> -p < src/main/resources/db/schema.sql
   ```

   If you have a DB from an earlier version, drop and recreate it.

2. **Connection config.** Each key resolves as `-Dkey`, then `$ENV_VAR`, then
   `src/main/resources/db.properties` on the classpath (the fallback when nothing
   else is set). `db.properties` is `.gitignored`; keep real production
   credentials out of it - for a deployment set env vars / `-D` and use
   `sslMode=REQUIRED`. `db.properties.example` is the template.

   | Key | Env var | Default |
   |-----|---------|---------|
   | `jdbc.url` | `JDBC_URL` | *(required)* |
   | `jdbc.user` | `JDBC_USER` | *(required)* |
   | `jdbc.password` | `JDBC_PASSWORD` | *(required)* |
   | `jdbc.pool.maxSize` | `JDBC_POOL_MAX_SIZE` | `10` |
   | `jdbc.pool.minIdle` | `JDBC_POOL_MIN_IDLE` | `2` |
   | `upload.dir` | `UPLOAD_DIR` | `<user.home>/c3s-admin-uploads` |
   | `session.cookie.secure` | `SESSION_COOKIE_SECURE` | `false` |
   | `log.dir` | `LOG_DIR` | `<catalina.base>/logs` |
   | `log.level` | - | `INFO` |

   Use `sslMode=REQUIRED` in the URL for anything real. A missing required key
   fails the deploy at startup with a clear message.

3. **Build:**

   ```
   ./mvnw clean package
   ```

   Produces `target/admin.war`. If `CATALINA_HOME` points at a Tomcat install,
   `mvn verify` also precompiles every JSP with Jasper (`build/verify-jsps.sh`)
   so a scriptlet error fails the build; without it that step is skipped.

4. **Deploy:** drop `target/admin.war` into the container's `webapps/`. Every
   page needs a session - an unauthenticated request goes to `/login`. After
   signing in, `/admin` and any unknown `/admin/...` URL land on
   `/admin/profiles`.

   Set `SESSION_COOKIE_SECURE=true` once you're behind HTTPS so the session
   cookie gets `Secure` (it's `HttpOnly; SameSite=Strict` either way).

   `schema.sql` seeds one admin: **username `admin`, password `admin123`** -
   change it after first login. After 5 failed logins for a username inside 15
   minutes, that username is locked out for 15 minutes (`LoginThrottle`); the
   response is 429.

## Pages

**Sign in**
- `GET /login` - the form (redirects to `/profiles` if already in). `/login`,
  `/css`, `/js` and `/healthz` are the only paths that don't need a session.
- `POST /login` (`username`, `password`, `_csrf`) - on success rotates to a
  fresh session and goes to `/profiles`; on a bad password re-renders with a
  generic error and 401; when locked out, 429.
- `POST /logout` - ends the session.

**Health**
- `GET /healthz` - runs a bounded `SELECT 1`. `200 {"status":"UP","db":"UP"}`
  when the pool is reachable, `503` otherwise. `Cache-Control: no-store`.
  Unauthenticated.

**Profiles**
- `GET /profiles` - the "All profiles" list (the start page).
- `POST /profiles/archive` (field `id`) - soft-delete: drops off the list, stays
  in the DB.
- `GET /profile` / `GET /profile?id=<id>` - blank or pre-filled form.
- `POST /profile` (`status=published` or `status=draft`, multipart) - validates
  and saves; inserts if there's no `id`, otherwise updates that row plus its
  category links and photo.

**Categories**
- `GET /categories` - every category, active and archived. Archived rows have a
  disabled Edit (blocked server-side too) and no Archive button.
- `POST /categories/archive` (field `id`) - archive; it drops out of the
  profile-form picker, but profiles already using it keep the link.
- `GET /category` / `GET /category?id=<id>` - add/edit form. Asking for an
  archived category's edit form bounces back to the list.
- `POST /category` - validates a unique label + a valid color, then saves.

Every category has a `color_class` (`cat-blue` / `cat-red` / `cat-gold` /
`cat-green` / `cat-purple` / `cat-neutral`), picked from a `<select>`.
`CategoryValidator` and a DB `CHECK` both enforce the set of six. `schema.sql`
seeds the six defaults with the prototype's colors.

**Allocation settings**
- `GET /allocations` - one table: the Default policy row on top, then each
  active category showing "Custom policy" or "Uses default", with an
  Edit / "+ Add" link into the form.
- `GET /allocation` - the default bucket split + fee policy, with a $50 sample
  preview and the donor-facing message it produces.
- `GET /allocation?categoryId=<id>` - that category's custom policy, or - if it
  has none - the default shown as an editable, unsaved template.
- `POST /allocation` (with `categoryId` if scoped) - validates and saves;
  creates the category's first custom policy if it didn't have one.

  The target `uwnefl_allocation_settings` row is always resolved server-side
  from `categoryId` - the client never sends a settings id. Internally a
  "custom policy" is still called an override in the DAO
  (`getOverride` / `getOverrideCategoryIds`).

## Photo upload

Profiles can have a photo (the `.photo-upload` dropzone).

- `GET /photos/<filename>` (`PhotoServlet`) - streams a stored photo. Only names
  matching the exact `<uuid>.<ext>` pattern `UploadUtil` generates are served;
  anything else (path traversal included) 404s without touching the filesystem.
  `nosniff` + `Cache-Control: private`.
- Files are checked by extension allowlist (jpg/jpeg/png/gif, 5MB max via
  `@MultipartConfig`) and by decoding the image header - a non-image, or one
  over 4000px on a side, is rejected before the pixels are read. Stored under
  `upload.dir`, outside the WAR, so they survive redeploys.
- Replacing or removing a photo deletes the old file; editing a profile without
  touching the photo leaves it alone.
- The browser shows a local preview before the upload happens on save.

## Validation

Server-side, in `ProfileValidator` (the form's `maxlength`/`required` exist too
but aren't trusted):

| Field | Rule |
|-------|------|
| name | required, ≤ 100 |
| blurb | required, ≤ 160 |
| story | optional, ≤ 2000 |
| categories | at least one |
| goal | required, whole number, ≥ 100 |
| raised | whole number ≥ 0, ≤ goal |
| deadline | optional, valid date, not in the past |
| state | optional, 2-letter code |

Failures re-render the form with per-field messages and the submitted values
kept (HTTP 422).

`CategoryValidator`: label required (≤ 100) and unique (checked against the DB;
the `UNIQUE` constraint backstops the race), color one of the six.

`AllocationValidator`: bucket shares total exactly 100%, every bucket named,
processor rate 0-10% and fixed fee $0-$5, exactly one bucket marked
to-recipient and at least one core bucket.

## Layout

```
servlet/  - HTTP: params, CSRF, dispatch, render
service/  - use cases; SidebarStatsCache + its invalidation; take DAOs via constructor
dao/      - JDBC (interface + Jdbc* impl per aggregate)
model/    - entities + pure calculators
web/      - PageData + per-page *PageData classes, Csrf, FlashScope
```

## Errors

Unexpected failures are logged server-side with full context and a stack trace;
the browser only ever sees a generic message via `error.jsp`. Both the servlets'
own `try/catch` and the `<error-page>` mappings in `web.xml` route there.
