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
 * POST /logout ends the donor scope, /admin/logout the admin one and /report/logout the report
 * one - one at a time, so signing out of one leaves the others alone. AuthSession.end drops the
 * session once no scope is left.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {Routes.BROWSE_LOGOUT, Routes.ADMIN_LOGOUT, Routes.REPORT_LOGOUT})
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
        return switch (req.getServletPath()) {
            case Routes.ADMIN_LOGOUT -> Scope.ADMIN;
            case Routes.REPORT_LOGOUT -> Scope.REPORT;
            default -> Scope.USER;
        };
    }

    private static String afterLogout(Scope scope) {
        return switch (scope) {
            case ADMIN -> Routes.ADMIN_LOGIN;
            case REPORT -> Routes.REPORT_LOGIN;
            case USER -> Routes.BROWSE_HOME;
        };
    }
}
