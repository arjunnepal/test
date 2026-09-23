package com.c3s.ai.core.servlet;

import com.c3s.ai.core.web.FlashScope;
import com.c3s.ai.core.web.PageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** What every page servlet in the app does the same way: render a view, redirect, read an id. */
public abstract class BaseServlet extends HttpServlet {

    // not in HttpServletResponse
    protected static final int SC_UNPROCESSABLE_ENTITY = 422;
    protected static final int SC_TOO_MANY_REQUESTS = 429;

    /** Forward to a JSP with its page object stored under the object's simple class name. */
    protected final void render(HttpServletRequest req, HttpServletResponse resp, String view, PageData data)
            throws ServletException, IOException {
        render(req, resp, view, data, HttpServletResponse.SC_OK);
    }

    protected final void render(HttpServletRequest req, HttpServletResponse resp, String view, PageData data,
                                int status) throws ServletException, IOException {
        req.setAttribute(data.attributeName(), data);
        resp.setStatus(status);
        req.getRequestDispatcher(view).forward(req, resp);
    }

    /** path is app-relative (a Routes constant); the context path is added here. */
    protected final void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    /** POST -> redirect -> GET with a one-shot message for the next page's toast. */
    protected final void redirectWithFlash(HttpServletRequest req, HttpServletResponse resp, String path,
                                           String message) throws IOException {
        FlashScope.push(req, message);
        redirect(req, resp, path);
    }

    protected static void noStore(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store");
    }

    /** A positive id, or null when the parameter is missing, blank or not a positive whole number. */
    protected static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(raw.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
