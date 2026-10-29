package com.c3s.ai.report.page;

import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.servlet.AbstractLoginServlet;
import com.c3s.ai.core.user.Role;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;

/**
 * The report sign-in: the staff sign-in page, but the REPORT scope. Signing in here doesn't open
 * the dashboard, signing in to the dashboard doesn't open the report, and signing out of either
 * leaves the other alone.
 */
@WebServlet(name = "ReportLoginServlet", urlPatterns = Routes.REPORT_LOGIN)
public class ReportLoginServlet extends AbstractLoginServlet {

    @Override
    protected String view() {
        return "/WEB-INF/views/admin/login.jsp";
    }

    @Override
    protected Scope scope() {
        return Scope.REPORT;
    }

    @Override
    protected String successPath() {
        return Routes.REPORT;
    }

    @Override
    protected boolean accepts(Role role) {
        return role.isAdministrator();
    }

    @Override
    protected String wrongDoorMessage() {
        return "That account doesn't have access to the financial report.";
    }
}
