package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.ErrorPageData;
import com.c3s.ai.admin.web.FlashScope;
import com.c3s.ai.admin.web.PageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Supplier;

/** Shared plumbing for the CRUD admin servlets - rendering, id parsing, load-or-error. */
abstract class AdminServlet extends HttpServlet {

    private static final String ERROR_VIEW = "/WEB-INF/views/error.jsp";

    // not in HttpServletResponse
    protected static final int SC_UNPROCESSABLE_ENTITY = 422;
    protected static final int SC_TOO_MANY_REQUESTS = 429;

    // stashes the page object under its simple class name, so the JSP reads it as
    // (LoginPageData) request.getAttribute("LoginPageData")
    protected void dispatch(HttpServletRequest req, HttpServletResponse resp, String view,
                            int status, PageData data)
            throws ServletException, IOException {
        req.setAttribute(data.getClass().getSimpleName(), data);
        if (status != HttpServletResponse.SC_OK) {
            resp.setStatus(status);
        }
        req.getRequestDispatcher(view).forward(req, resp);
    }

    /** 200 render, picking up any flash left by a prior redirect. */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view, PageData data)
            throws ServletException, IOException {
        String flash = FlashScope.pull(req);
        if (flash != null) {
            data.setMessage(flash);
        }
        dispatch(req, resp, view, HttpServletResponse.SC_OK, data);
    }

    protected void renderRejected(HttpServletRequest req, HttpServletResponse resp, String view,
                                  PageData data, String message)
            throws ServletException, IOException {
        renderRejected(req, resp, view, data, message, SC_UNPROCESSABLE_ENTITY);
    }

    /** status is 401 (bad login), 422 (validation) or 429 (throttled). */
    protected void renderRejected(HttpServletRequest req, HttpServletResponse resp, String view,
                                  PageData data, String message, int status)
            throws ServletException, IOException {
        data.setMessage(message);
        dispatch(req, resp, view, status, data);
    }

    protected void renderError(HttpServletRequest req, HttpServletResponse resp, int code, String message)
            throws ServletException, IOException {
        ErrorPageData data = new ErrorPageData();
        data.setMessage(message);
        dispatch(req, resp, ERROR_VIEW, code, data);
    }

    // message differs per entity ("profile" / "category")
    protected void showNotFound(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        renderError(req, resp, HttpServletResponse.SC_NOT_FOUND, message);
    }

    protected void showServerError(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        renderError(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Something went wrong on our end. Please try again shortly.");
    }

    protected static String trim(String s) {
        return s == null ? null : s.trim();
    }

    protected static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Parsed id, or null if missing/blank/not a long. Never throws. */
    protected static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FunctionalInterface
    protected interface EntityAction<T> {
        T run() throws EntityNotFoundException;
    }

    // load/mutate one entity: not-found -> 404 page, anything else -> logged + 500 page.
    // returns null in both cases, so the caller has to check and bail.
    protected <T> T loadOrRespond(HttpServletRequest req, HttpServletResponse resp, Logger log,
                                   String what, String notFoundMessage, EntityAction<T> action)
            throws ServletException, IOException {
        try {
            return action.run();
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

    /** Read-only load: failure -> logged + 500 page, returns null (caller must bail). */
    protected <T> T loadListOrRespond(HttpServletRequest req, HttpServletResponse resp, Logger log,
                                       String what, Supplier<T> action)
            throws ServletException, IOException {
        try {
            return action.get();
        } catch (RuntimeException e) {
            log.error("Failed: {}", Log.clean(what), e);
            showServerError(req, resp);
            return null;
        }
    }
}
