package com.c3s.ai.core.web;

import java.util.regex.Pattern;

/** Every path in the app, shared by the filters, the servlets, the JSPs and the error page. */
public final class Routes {

    public static final String ADMIN_PREFIX = "/admin";

    // giving site
    public static final String BROWSE_HOME = "/";
    /** The giving page's own path; "/" reaches it through the index.jsp welcome file. */
    public static final String BROWSE_PAGE = "/browse";
    public static final String BROWSE_LOGIN = "/login";
    public static final String BROWSE_LOGOUT = "/logout";

    // dashboard
    public static final String ADMIN_LOGIN = "/admin/login";
    public static final String ADMIN_LOGOUT = "/admin/logout";
    public static final String ADMIN_HOME = "/admin/profiles";
    public static final String ADMIN_PROFILES = "/admin/profiles";
    public static final String ADMIN_PROFILE = "/admin/profile";
    public static final String ADMIN_PROFILES_ARCHIVE = "/admin/profiles/archive";
    public static final String ADMIN_CATEGORIES = "/admin/categories";
    public static final String ADMIN_CATEGORY = "/admin/category";
    public static final String ADMIN_CATEGORIES_ARCHIVE = "/admin/categories/archive";
    public static final String ADMIN_ALLOCATIONS = "/admin/allocations";
    public static final String ADMIN_ALLOCATION = "/admin/allocation";
    public static final String PHOTOS_PREFIX = "/photos/";

    // financial report - its own sign-in and session scope, ADMINISTRATOR only
    public static final String REPORT = "/report";
    public static final String REPORT_LOGIN = "/report/login";
    public static final String REPORT_LOGOUT = "/report/logout";

    public static final String HEALTH = "/healthz";

    // lower-case segments only, so a return path can't leave the site or carry a query
    private static final Pattern ADMIN_PAGE = Pattern.compile("/admin(/[a-z0-9-]+)+");

    private Routes() {
    }

    /** True for /admin and anything under it - but not, say, /administrators. */
    public static boolean isAdminPath(String path) {
        return path != null
                && (path.equals(ADMIN_PREFIX) || path.startsWith(ADMIN_PREFIX + "/"));
    }

    /** True for /report and anything under it. */
    public static boolean isReportPath(String path) {
        return path != null && (path.equals(REPORT) || path.startsWith(REPORT + "/"));
    }

    /**
     * An /admin page to go back to after signing in, or null. Anything else - another site, the
     * sign-in or sign-out itself, a malformed path - is refused, so it can't be an open redirect.
     */
    public static String adminReturnPath(String path) {
        if (path == null || !ADMIN_PAGE.matcher(path).matches()
                || path.equals(ADMIN_LOGIN) || path.equals(ADMIN_LOGOUT)) {
            return null;
        }
        return path;
    }

    public static boolean isStaticAsset(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/");
    }
}
