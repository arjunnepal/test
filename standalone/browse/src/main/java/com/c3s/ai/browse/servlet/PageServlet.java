package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.web.ErrorPageData;
import com.c3s.ai.browse.web.PageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Shared rendering for the servlets that forward to a JSP. Same shape as admin's AdminServlet.
abstract class PageServlet extends HttpServlet {

    private static final String ERROR_VIEW = "/WEB-INF/views/error.jsp";

    // not in HttpServletResponse
    protected static final int SC_TOO_MANY_REQUESTS = 429;

    // stores the page object under its simple class name, so the JSP reads it as
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

    // status is 401 (bad login) or 429 (throttled)
    protected void renderRejected(HttpServletRequest req, HttpServletResponse resp, String view,
                                  PageData data, String message, int status)
            throws ServletException, IOException {
        data.setMessage(message);
        dispatch(req, resp, view, status, data);
    }

    // generic error page with a 500, log the cause before calling it
    protected void showServerError(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ErrorPageData data = new ErrorPageData();
        data.setMessage("Something went wrong on our end. Please try again shortly.");
        dispatch(req, resp, ERROR_VIEW, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, data);
    }

    protected static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
