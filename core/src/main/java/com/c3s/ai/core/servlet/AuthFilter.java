package com.c3s.ai.core.servlet;

import com.c3s.ai.common.util.Log;
import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.user.Role;
import com.c3s.ai.core.auth.Csrf;
import com.c3s.ai.core.web.FlashScope;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * The one gate in front of both halves of the app, ordered in web.xml. Static assets and /healthz
 * pass, every non-GET needs a valid CSRF token, and the public site needs no session at all.
 * Everything under /admin and /photos needs the ADMIN scope and a staff role - the USER scope
 * never opens an admin door. EMPLOYEE has an allow-list, so a new admin path is locked by default.
 */
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** /admin paths an EMPLOYEE may use, in addition to /photos/*. */
    private static final Set<String> EMPLOYEE_PATHS = Set.of(
            Routes.ADMIN_PREFIX, Routes.ADMIN_PROFILES, Routes.ADMIN_PROFILE,
            Routes.ADMIN_PROFILES_ARCHIVE, Routes.ADMIN_LOGOUT);

    static final String FORM_EXPIRED_MESSAGE = "That form had expired, so nothing was saved. Please try again.";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // servletPath+pathInfo is already decoded and normalized, unlike getRequestURI()
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String path = pathInfo == null ? servletPath : servletPath + pathInfo;

        if (Routes.isStaticAsset(path) || path.equals(Routes.HEALTH)) {
            chain.doFilter(request, response);
            return;
        }

        boolean adminPath = Routes.isAdminPath(path) || path.startsWith(Routes.PHOTOS_PREFIX);
        if (adminPath) {
            // recipient data and CSRF tokens must not survive in the browser cache after sign-out;
            // PhotoServlet sets its own, shorter private caching
            resp.setHeader("Cache-Control", "no-store");
        }

        // POST /admin/profile is multipart - its _csrf is a form part this filter can't read
        // before the parts are parsed, so ProfileServlet checks it after parsing.
        boolean multipartProfilePost =
                "POST".equalsIgnoreCase(req.getMethod()) && path.equals(Routes.ADMIN_PROFILE);

        if (!isSafeMethod(req.getMethod()) && !multipartProfilePost && !Csrf.matches(req)) {
            csrfReject(req, resp, path, adminPath);
            return;
        }

        if (!adminPath) {
            // Public site. A donor session is optional; keep its idle clock alive if present.
            AuthSession.touch(req, Scope.USER);
            chain.doFilter(request, response);
            return;
        }

        if (path.equals(Routes.ADMIN_LOGIN)) {
            chain.doFilter(request, response);
            return;
        }

        Role role = AuthSession.currentRole(req, Scope.ADMIN);
        if (role == null) {
            // no admin scope, or it expired - the donor scope is deliberately no help here
            log.info("No admin session for {}, redirecting to the admin sign-in", Log.clean(path));
            resp.sendRedirect(req.getContextPath() + Routes.ADMIN_LOGIN);
            return;
        }

        if (!role.isStaff() || (!role.isAdministrator() && !employeeMayUse(path))) {
            log.warn("Role {} refused for {}", role, Log.clean(path));
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // only /admin traffic slides the admin scope's shorter idle window
        AuthSession.touch(req, Scope.ADMIN);
        chain.doFilter(request, response);
    }

    static boolean employeeMayUse(String path) {
        return EMPLOYEE_PATHS.contains(path) || path.startsWith(Routes.PHOTOS_PREFIX);
    }

    private static boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    /**
     * A missing or stale CSRF token, usually a form left open too long. Nothing is saved, and
     * nobody is signed out - that would let any same-site page end a session with one POST.
     */
    private static void csrfReject(HttpServletRequest req, HttpServletResponse resp, String path, boolean adminPath)
            throws IOException {
        log.info("Missing/invalid CSRF token for {} {}", Log.clean(req.getMethod()), Log.clean(path));
        if (path.equals(Routes.ADMIN_LOGIN) || path.equals(Routes.BROWSE_LOGIN)) {
            resp.sendRedirect(req.getContextPath() + path);
            return;
        }
        FlashScope.push(req, FORM_EXPIRED_MESSAGE);
        resp.sendRedirect(req.getContextPath() + (adminPath ? Routes.ADMIN_HOME : Routes.BROWSE_HOME));
    }
}
