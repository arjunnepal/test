package com.c3s.ai.report.page;

import com.c3s.ai.core.servlet.BaseServlet;
import com.c3s.ai.core.servlet.ErrorPages;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * GET /report - the financial report. AuthFilter has already required the report scope and the
 * ADMINISTRATOR role and marked the response no-store. Read-only; the what-if controls on the
 * page never post.
 */
@WebServlet(name = "ReportServlet", urlPatterns = Routes.REPORT)
public class ReportServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(ReportServlet.class);
    private static final String VIEW = "/WEB-INF/views/report/report.jsp";

    private final ReportService service = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ReportPageData data;
        try {
            data = service.buildPage();
        } catch (RuntimeException e) {
            log.error("Failed to build the financial report", e);
            ErrorPages.serverError(req, resp);
            return;
        }
        render(req, resp, VIEW, data);
    }
}
