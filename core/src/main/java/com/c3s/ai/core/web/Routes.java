package com.c3s.ai.core.web;

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

    public static final String HEALTH = "/healthz";

    private Routes() {
    }

    /** True for /admin and anything under it - but not, say, /administrators. */
    public static boolean isAdminPath(String path) {
        return path != null
                && (path.equals(ADMIN_PREFIX) || path.startsWith(ADMIN_PREFIX + "/"));
    }

    public static boolean isStaticAsset(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/");
    }
}
