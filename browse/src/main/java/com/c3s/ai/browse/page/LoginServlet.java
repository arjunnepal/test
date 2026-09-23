package com.c3s.ai.browse.page;

import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.servlet.AbstractLoginServlet;
import com.c3s.ai.core.user.Role;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;

/**
 * The donor sign-in. Opens the USER scope only - an administrator signing in here gets a
 * donor session and nothing more; the dashboard is a separate sign-in at /admin/login.
 */
@WebServlet(name = "LoginServlet", urlPatterns = Routes.BROWSE_LOGIN)
public class LoginServlet extends AbstractLoginServlet {

    @Override
    protected String view() {
        return "/WEB-INF/views/browse/login.jsp";
    }

    @Override
    protected Scope scope() {
        return Scope.USER;
    }

    @Override
    protected String successPath() {
        return Routes.BROWSE_HOME;
    }

    @Override
    protected boolean accepts(Role role) {
        return true;   // every role may use the public site
    }

    @Override
    protected String wrongDoorMessage() {
        throw new UnsupportedOperationException("the donor sign-in turns nobody away");
    }
}
