package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.util.Log;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// 404 target (web.xml error-page). An unknown URL just bounces to /profiles.
// Real failures still go to error.jsp - this is only "no such page".
@WebServlet(name = "NotFoundServlet", urlPatterns = "/not-found")
public class NotFoundServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(NotFoundServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Object requestedUri = req.getAttribute("jakarta.servlet.error.request_uri");
        log.debug("404, defaulting to /profiles (requested: {})", Log.clean(requestedUri));
        resp.sendRedirect(req.getContextPath() + "/profiles");
    }
}
