package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.auth.AuthService;
import com.c3s.ai.browse.auth.AuthSession;
import com.c3s.ai.browse.auth.LoginThrottle;
import com.c3s.ai.browse.model.User;
import com.c3s.ai.browse.util.Log;
import com.c3s.ai.browse.web.Csrf;
import com.c3s.ai.browse.web.LoginPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

// GET /login  - the sign-in form (straight back to the browse page if already signed in)
// POST /login - check the credentials, start a session, back to the browse page
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends PageServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);
    private static final String VIEW = "/WEB-INF/views/login.jsp";

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (AuthSession.isAuthenticated(req)) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        noStore(resp);
        dispatch(req, resp, VIEW, HttpServletResponse.SC_OK, new LoginPageData());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        if (!Csrf.matches(req)) {
            // usually an expired session
            log.info("Login POST without a valid CSRF token, showing the form again");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        // the proxy's address unless RemoteIpValve is configured
        String client = req.getRemoteAddr();

        try {
            if (LoginThrottle.BY_USERNAME.isLocked(username) || LoginThrottle.BY_CLIENT.isLocked(client)) {
                log.warn("Login blocked (throttled) for username={} client={}",
                        Log.clean(username), Log.clean(client));
                reject(req, resp, username, "Too many failed attempts. Try again in a few minutes.",
                        SC_TOO_MANY_REQUESTS);
                return;
            }

            Optional<User> user = authService.authenticate(username, password);
            if (user.isEmpty()) {
                LoginThrottle.BY_USERNAME.recordFailure(username);
                LoginThrottle.BY_CLIENT.recordFailure(client);
                log.info("Failed login attempt for username={} client={}", Log.clean(username), Log.clean(client));
                if (LoginThrottle.BY_USERNAME.isLocked(username) || LoginThrottle.BY_CLIENT.isLocked(client)) {
                    log.warn("Too many failed logins, locked out username={} client={}",
                            Log.clean(username), Log.clean(client));
                }
                reject(req, resp, username, "Invalid username or password.", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // keep the client count
            LoginThrottle.BY_USERNAME.reset(username);
            AuthSession.begin(req, user.get());
            log.info("Login succeeded for username={}", Log.clean(user.get().getUsername()));
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (RuntimeException e) {
            log.error("Login failed with an unexpected error", e);
            showServerError(req, resp);
        }
    }

    // the form again, keeping the username
    private void reject(HttpServletRequest req, HttpServletResponse resp, String username,
                        String message, int status) throws ServletException, IOException {
        LoginPageData data = new LoginPageData();
        data.setUsername(username);
        noStore(resp);
        renderRejected(req, resp, VIEW, data, message, status);
    }

    // the form has a CSRF token, don't cache
    private static void noStore(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-store");
    }
}
