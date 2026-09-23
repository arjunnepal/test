package com.c3s.ai.core.servlet;

import com.c3s.ai.core.web.PageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The one error page for both halves of the app; error.jsp picks its wording from whether an admin
 * session is live. Log the cause before calling these - the page never shows it.
 */
public final class ErrorPages {

    public static final String VIEW = "/WEB-INF/views/common/error.jsp";

    private ErrorPages() {
    }

    public static void render(HttpServletRequest req, HttpServletResponse resp, int status, String message)
            throws ServletException, IOException {
        PageData data = new PageData();
        data.setMessage(message);
        req.setAttribute(data.attributeName(), data);
        resp.setStatus(status);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    public static void serverError(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Something went wrong on our end. Please try again shortly.");
    }

    public static void notFound(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        render(req, resp, HttpServletResponse.SC_NOT_FOUND, message);
    }
}
