package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.auth.AuthService;
import com.c3s.ai.admin.auth.AuthSession;
import com.c3s.ai.admin.auth.LoginThrottle;
import com.c3s.ai.admin.model.User;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.LoginPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

// GET /login - the form (skip to /profiles if already in)
// POST /login - check credentials, start a session, go to /profiles
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);
    private static final String VIEW = "/WEB-INF/views/login.jsp";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (AuthSession.isAuthenticated(req)) {
            resp.sendRedirect(req.getContextPath() + "/profiles");
            return;
        }
        render(req, resp, VIEW, new LoginPageData());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");

        if (LoginThrottle.isLocked(username)) {
            log.warn("Login blocked (throttled) for username={}", Log.clean(username));
            LoginPageData data = new LoginPageData();
            data.setUsername(username);
            renderRejected(req, resp, VIEW, data,
                    "Too many failed attempts. Try again in a few minutes.", SC_TOO_MANY_REQUESTS);
            return;
        }

        Optional<User> user = authService.authenticate(username, password);
        if (user.isEmpty()) {
            LoginThrottle.recordFailure(username);
            log.info("Failed login attempt for username={}", Log.clean(username));
            LoginPageData data = new LoginPageData();
            data.setUsername(username);
            renderRejected(req, resp, VIEW, data, "Invalid username or password.",
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        LoginThrottle.reset(username);
        AuthSession.begin(req, user.get().getUsername());
        log.info("Login succeeded for username={}", user.get().getUsername());
        resp.sendRedirect(req.getContextPath() + "/profiles");
    }
}
