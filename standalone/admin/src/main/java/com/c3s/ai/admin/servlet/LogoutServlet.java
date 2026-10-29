package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.auth.AuthSession;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// POST /logout - kill the session, back to /login
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = AuthSession.currentUser(req);
        AuthSession.end(req);
        if (user != null) {
            log.info("Logout for username={}", user);
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
