package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.auth.AuthSession;
import com.c3s.ai.browse.util.Log;
import com.c3s.ai.browse.web.Csrf;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// POST /logout - end the session and go back to the browse page
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Csrf.matches(req)) {
            String user = AuthSession.currentUser(req);
            AuthSession.end(req);
            if (user != null) {
                log.info("Logout for username={}", Log.clean(user));
            }
        } else {
            log.info("Logout POST without a valid CSRF token, ignored");
        }
        resp.sendRedirect(req.getContextPath() + "/");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/");
    }
}
