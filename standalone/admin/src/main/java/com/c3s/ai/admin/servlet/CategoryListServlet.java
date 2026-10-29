package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.service.CategoryService;
import com.c3s.ai.admin.web.CategoryListPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

// GET /categories - every category, active and archived
@WebServlet(name = "CategoryListServlet", urlPatterns = "/categories")
public class CategoryListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(CategoryListServlet.class);
    private static final String LIST_VIEW = "/WEB-INF/views/categories-list.jsp";

    private final CategoryService service = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Category> categories = loadListOrRespond(req, resp, log, "category list", service::listAll);
        if (categories == null) {
            return;
        }
        CategoryListPageData data = new CategoryListPageData();
        data.setCategories(categories);
        render(req, resp, LIST_VIEW, data);
    }
}
