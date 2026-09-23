package com.c3s.ai.core.servlet;

import com.c3s.ai.common.util.Log;
import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.user.AuthService;
import com.c3s.ai.core.user.AuthenticatedUser;
import com.c3s.ai.core.user.LoginResult;
import com.c3s.ai.core.user.Role;
import com.c3s.ai.core.web.LoginPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * All sign-in behaviour, once; subclasses supply the view, the scope, the roles that door accepts
 * and where they land. AuthFilter has already checked the CSRF token by the time doPost runs.
 */
public abstract class AbstractLoginServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoginServlet.class);

    private final AuthService authService;

    protected AbstractLoginServlet() {
        this(new AuthService());
    }

    // tests hand in a stub
    protected AbstractLoginServlet(AuthService authService) {
        this.authService = authService;
    }

    /** The JSP for this door. */
    protected abstract String view();

    /** Which session scope a successful sign-in here opens. */
    protected abstract Scope scope();

    /** Where to go once signed in. */
    protected abstract String successPath();

    /** False rejects the credentials even though they were correct (wrong door). */
    protected abstract boolean accepts(Role role);

    /** Shown when accepts() says no. */
    protected abstract String wrongDoorMessage();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (AuthSession.isAuthenticated(req, scope())) {
            redirect(req, resp, successPath());
            return;
        }
        show(req, resp, new LoginPageData(), HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // the proxy's address unless RemoteIpValve is configured
        String client = req.getRemoteAddr();

        try {
            LoginResult result = authService.login(username, password, client);
            switch (result) {
                case LoginResult.Throttled t -> {
                    log.warn("Login blocked (throttled) for username={} client={}", Log.clean(username), Log.clean(client));
                    reject(req, resp, username, "Too many attempts. Try again in a few minutes.", SC_TOO_MANY_REQUESTS);
                }
                case LoginResult.Rejected r -> {
                    log.info("Failed login attempt for username={} client={}", Log.clean(username), Log.clean(client));
                    reject(req, resp, username, "Invalid username or password.", HttpServletResponse.SC_UNAUTHORIZED);
                }
                case LoginResult.Success s -> signIn(req, resp, username, s.user());
            }
        } catch (RuntimeException e) {
            log.error("Login failed with an unexpected error", e);
            ErrorPages.serverError(req, resp);
        }
    }

    private void signIn(HttpServletRequest req, HttpServletResponse resp, String typedUsername, AuthenticatedUser user)
            throws ServletException, IOException {
        if (!accepts(user.role())) {
            // The password was right, so saying this leaks nothing they don't already
            // know - and it beats silently bouncing them somewhere they didn't ask for.
            log.info("Role {} refused at the {} sign-in for username={}", user.role(), scope(), Log.clean(user.username()));
            reject(req, resp, typedUsername, wrongDoorMessage(), HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        AuthSession.begin(req, scope(), user);
        log.info("Login succeeded for username={} scope={} role={}", Log.clean(user.username()), scope(), user.role());
        redirect(req, resp, successPath());
    }

    private void reject(HttpServletRequest req, HttpServletResponse resp, String username,
                        String message, int status) throws ServletException, IOException {
        LoginPageData data = new LoginPageData();
        data.setUsername(username == null ? null : username.trim());
        data.setMessage(message);
        show(req, resp, data, status);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, LoginPageData data, int status)
            throws ServletException, IOException {
        // has a CSRF token, don't cache
        noStore(resp);
        render(req, resp, view(), data, status);
    }
}
