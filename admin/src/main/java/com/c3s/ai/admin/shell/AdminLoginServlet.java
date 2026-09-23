package com.c3s.ai.admin.shell;

import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.servlet.AbstractLoginServlet;
import com.c3s.ai.core.user.Role;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;

/**
 * The staff sign-in: its own page, the same authentication code as the donor one, and the ADMIN
 * scope, which is the only thing AuthFilter accepts under /admin.
 */
@WebServlet(name = "AdminLoginServlet", urlPatterns = Routes.ADMIN_LOGIN)
public class AdminLoginServlet extends AbstractLoginServlet {

    @Override
    protected String view() {
        return "/WEB-INF/views/admin/login.jsp";
    }

    @Override
    protected Scope scope() {
        return Scope.ADMIN;
    }

    @Override
    protected String successPath() {
        return Routes.ADMIN_HOME;
    }

    @Override
    protected boolean accepts(Role role) {
        return role.isStaff();
    }

    @Override
    protected String wrongDoorMessage() {
        return "That account doesn't have access to the admin dashboard.";
    }
}
