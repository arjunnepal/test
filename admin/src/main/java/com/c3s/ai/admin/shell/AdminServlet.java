package com.c3s.ai.admin.shell;

import com.c3s.ai.admin.profile.SidebarStats;
import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.common.util.Log;
import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Principal;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.servlet.BaseServlet;
import com.c3s.ai.core.servlet.ErrorPages;
import com.c3s.ai.core.web.AppCaches;
import com.c3s.ai.core.web.FlashScope;
import com.c3s.ai.core.web.PageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Supplier;

/** Shared plumbing for the dashboard servlets: the page shell, flash messages and load-or-error. */
public abstract class AdminServlet extends BaseServlet {

    public static final String SERVER_ERROR_MESSAGE = "Something went wrong on our end. Please try again shortly.";

    /** Render a dashboard page, picking up any flash left by a prior redirect. */
    protected final void show(HttpServletRequest req, HttpServletResponse resp, String view, PageData data)
            throws ServletException, IOException {
        String flash = FlashScope.pull(req);
        if (flash != null) {
            data.setMessage(flash);
        }
        showWithStatus(req, resp, view, data, HttpServletResponse.SC_OK);
    }

    /** Re-render a form that failed validation (422) with a toast message. */
    protected final void showRejected(HttpServletRequest req, HttpServletResponse resp, String view, PageData data,
                                      String message) throws ServletException, IOException {
        data.setMessage(message);
        showWithStatus(req, resp, view, data, SC_UNPROCESSABLE_ENTITY);
    }

    private void showWithStatus(HttpServletRequest req, HttpServletResponse resp, String view, PageData data,
                                int status) throws ServletException, IOException {
        req.setAttribute(AdminShell.class.getSimpleName(), shell(req));
        render(req, resp, view, data, status);
    }

    protected final void showNotFound(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        ErrorPages.notFound(req, resp, message);
    }

    protected final void showServerError(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ErrorPages.serverError(req, resp);
    }

    /** After any dashboard write: clear the caches that might now show stale data (sidebar, giving page). */
    protected final void writesDone(HttpServletRequest req) {
        AppCaches.invalidateAll(req.getServletContext());
    }

    @FunctionalInterface
    protected interface Loader<T> {
        T load() throws EntityNotFoundException;
    }

    @FunctionalInterface
    protected interface EditableLoader<T> {
        T load() throws EntityNotFoundException, ArchivedEntityException;
    }

    // Load one entity: not found -> 404 page, anything else -> logged + 500 page. Returns null in
    // both cases, so the caller has to check and stop.
    protected final <T> T loadOrRespond(HttpServletRequest req, HttpServletResponse resp, Logger log,
                                        String what, String notFoundMessage, Loader<T> loader)
            throws ServletException, IOException {
        try {
            return loader.load();
        } catch (EntityNotFoundException e) {
            log.warn("Not found: {}", Log.clean(what));
            showNotFound(req, resp, notFoundMessage);
            return null;
        } catch (RuntimeException e) {
            log.error("Failed: {}", Log.clean(what), e);
            showServerError(req, resp);
            return null;
        }
    }

    /** As loadOrRespond, and an archived entity redirects back to its list with a notice. */
    protected final <T> T loadEditableOrRespond(HttpServletRequest req, HttpServletResponse resp, Logger log,
                                                String what, String notFoundMessage, String listPath,
                                                String entityPlural, EditableLoader<T> loader)
            throws ServletException, IOException {
        try {
            return loader.load();
        } catch (ArchivedEntityException e) {
            log.info("Blocked edit of archived {}", Log.clean(what));
            redirectWithFlash(req, resp, listPath, FlashMessages.blockedEdit(entityPlural));
            return null;
        } catch (EntityNotFoundException e) {
            log.warn("Not found: {}", Log.clean(what));
            showNotFound(req, resp, notFoundMessage);
            return null;
        } catch (RuntimeException e) {
            log.error("Failed: {}", Log.clean(what), e);
            showServerError(req, resp);
            return null;
        }
    }

    /** Read-only load: failure -> logged + 500 page, returns null (caller must stop). */
    protected final <T> T loadListOrRespond(HttpServletRequest req, HttpServletResponse resp, Logger log,
                                            String what, Supplier<T> loader)
            throws ServletException, IOException {
        try {
            return loader.get();
        } catch (RuntimeException e) {
            log.error("Failed: {}", Log.clean(what), e);
            showServerError(req, resp);
            return null;
        }
    }

    protected static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static AdminShell shell(HttpServletRequest req) {
        Principal principal = AuthSession.current(req, Scope.ADMIN);
        SidebarStats stats = SidebarStatsCache.INSTANCE.get();
        if (principal == null) {
            return new AdminShell(null, "?", "Not signed in", false, stats.publishedProfiles(), stats.totalRaised());
        }
        return new AdminShell(principal.username(), AdminShell.initialsOf(principal.username()),
                principal.role().label(), principal.role().isAdministrator(),
                stats.publishedProfiles(), stats.totalRaised());
    }
}
