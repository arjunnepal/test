package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.admin.shell.AdminServlet;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** GET /admin/allocations - default policy row + each active category's override status */
@WebServlet(name = "AllocationListServlet", urlPatterns = Routes.ADMIN_ALLOCATIONS)
public class AllocationListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AllocationListServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/allocation-list.jsp";

    private final AllocationService service = new AllocationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<CategoryRef> categories = loadListOrRespond(req, resp, log, "category list", service::activeCategories);
        if (categories == null) {
            return;
        }
        Set<Long> overrides = loadListOrRespond(req, resp, log, "allocation override category ids",
                service::overrideCategoryIds);
        if (overrides != null) {
            show(req, resp, VIEW, new AllocationListPageData(categories, overrides));
        }
    }
}
