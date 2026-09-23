package com.c3s.ai.core.servlet;

import com.c3s.ai.common.util.Log;
import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * POST /logout ends the donor scope, POST /admin/logout the admin one - one at a time, so signing
 * out of the dashboard leaves a donor session alone. AuthSession.end drops the session after both.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {Routes.BROWSE_LOGOUT, Routes.ADMIN_LOGOUT})
public class LogoutServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Scope scope = scopeFor(req);
        String user = AuthSession.currentUser(req, scope);
        AuthSession.end(req, scope);
        if (user != null) {
            log.info("Logout for username={} scope={}", Log.clean(user), scope);
        }
        redirect(req, resp, afterLogout(scope));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        redirect(req, resp, afterLogout(scopeFor(req)));
    }

    private static Scope scopeFor(HttpServletRequest req) {
        return Routes.ADMIN_LOGOUT.equals(req.getServletPath()) ? Scope.ADMIN : Scope.USER;
    }

    private static String afterLogout(Scope scope) {
        return scope == Scope.ADMIN ? Routes.ADMIN_LOGIN : Routes.BROWSE_HOME;
    }
}
