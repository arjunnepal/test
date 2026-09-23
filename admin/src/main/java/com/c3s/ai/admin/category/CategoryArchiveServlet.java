package com.c3s.ai.admin.category;

import com.c3s.ai.admin.shell.ArchiveServlet;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;

/**
 * POST /admin/categories/archive (field "id") - soft-delete a category. Profiles already linked
 * to it keep the link; it drops out of the profile-form picker and off the giving page.
 */
@WebServlet(name = "CategoryArchiveServlet", urlPatterns = Routes.ADMIN_CATEGORIES_ARCHIVE)
public class CategoryArchiveServlet extends ArchiveServlet {

    private final CategoryService service = new CategoryService();

    @Override
    protected String entityLabel() {
        return "Category";
    }

    @Override
    protected String listPath() {
        return Routes.ADMIN_CATEGORIES;
    }

    @Override
    protected String notFoundMessage() {
        return CategoryServlet.NOT_FOUND_MESSAGE;
    }

    @Override
    protected String archive(long id) throws EntityNotFoundException {
        return service.archive(id);
    }
}
