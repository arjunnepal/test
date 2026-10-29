package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.service.AllocationService;
import com.c3s.ai.admin.web.AllocationListPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// GET /allocations - default policy row + each active category's override status
@WebServlet(name = "AllocationListServlet", urlPatterns = "/allocations")
public class AllocationListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AllocationListServlet.class);
    private static final String VIEW = "/WEB-INF/views/allocation-list.jsp";

    private final AllocationService service = new AllocationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Category> categories = loadListOrRespond(req, resp, log, "category list", service::activeCategories);
        if (categories == null) {
            return;
        }
        Set<Long> overrideCategoryIds = loadListOrRespond(req, resp, log, "allocation override category ids",
                service::overrideCategoryIds);
        if (overrideCategoryIds == null) {
            return;
        }

        Map<Long, Boolean> categoryHasOverride = new LinkedHashMap<>();
        for (Category c : categories) {
            categoryHasOverride.put(c.getId(), overrideCategoryIds.contains(c.getId()));
        }
        AllocationListPageData data = new AllocationListPageData();
        data.setCategories(categories);
        data.setCategoryHasOverride(categoryHasOverride);
        render(req, resp, VIEW, data);
    }
}
