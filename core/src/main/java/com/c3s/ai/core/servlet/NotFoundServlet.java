package com.c3s.ai.core.servlet;

import com.c3s.ai.common.util.Log;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 404 target (web.xml error-page). An unknown URL bounces to the home page of whichever
 * half it was aimed at. Real failures still go to error.jsp - this is only "no such page".
 */
@WebServlet(name = "NotFoundServlet", urlPatterns = "/not-found")
public class NotFoundServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NotFoundServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Object requestedUri = req.getAttribute("jakarta.servlet.error.request_uri");
        String target = isAdminRequest(req, requestedUri) ? Routes.ADMIN_HOME : Routes.BROWSE_HOME;
        log.debug("404, defaulting to {} (requested: {})", target, Log.clean(requestedUri));
        resp.sendRedirect(req.getContextPath() + target);
    }

    // the original URI still carries the context path, so compare after stripping it
    private boolean isAdminRequest(HttpServletRequest req, Object requestedUri) {
        if (!(requestedUri instanceof String uri)) {
            return false;
        }
        String ctx = req.getContextPath();
        String path = !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        return Routes.isAdminPath(path);
    }
}
