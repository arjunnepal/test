package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.service.CategoryService;
import com.c3s.ai.admin.util.FlashMessages;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.FlashScope;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// POST /categories/archive (field "id") - soft-delete a category. Profiles already
// linked to it keep the link; it just drops out of the profile-form picker.
@WebServlet(name = "CategoryArchiveServlet", urlPatterns = "/categories/archive")
public class CategoryArchiveServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(CategoryArchiveServlet.class);
    private static final String NOT_FOUND_MESSAGE = "That category could not be found.";

    private final CategoryService service = new CategoryService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Long id = parseId(idParam);
        if (id == null) {
            log.warn("Archive requested for malformed category id={}", Log.clean(idParam));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        }

        String label = loadOrRespond(req, resp, log, "category id=" + id, NOT_FOUND_MESSAGE,
                () -> service.archive(id));
        if (label == null) {
            return;
        }
        log.info("Archived category id={}", id);

        FlashScope.push(req, FlashMessages.archived("Category", label));
        resp.sendRedirect(req.getContextPath() + "/categories");
    }
}
