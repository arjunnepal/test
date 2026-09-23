package com.c3s.ai.admin.category;

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

/** GET /admin/categories - every category, active and archived */
@WebServlet(name = "CategoryListServlet", urlPatterns = Routes.ADMIN_CATEGORIES)
public class CategoryListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(CategoryListServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/categories-list.jsp";

    private final CategoryService service = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Category> categories = loadListOrRespond(req, resp, log, "category list", service::listAll);
        if (categories != null) {
            show(req, resp, VIEW, new CategoryListPageData(categories));
        }
    }
}
