package com.c3s.ai.admin.category;

import com.c3s.ai.admin.shell.AdminServlet;
import com.c3s.ai.admin.shell.FlashMessages;
import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.common.util.Log;
import com.c3s.ai.common.util.Strings;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/** GET /admin/category [?id=X] - add/edit form. POST /admin/category - validate and save. */
@WebServlet(name = "CategoryServlet", urlPatterns = Routes.ADMIN_CATEGORY)
public class CategoryServlet extends AdminServlet {

    static final String NOT_FOUND_MESSAGE = "That category could not be found.";

    private static final Logger log = LoggerFactory.getLogger(CategoryServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/category-form.jsp";

    private final CategoryService service = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (Strings.isBlank(idParam)) {
            renderForm(req, resp, new Category(), false, null);
            return;
        }
        Long id = parseId(idParam);
        if (id == null) {
            log.warn("Edit requested for malformed category id={}", Log.clean(idParam));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        }
        Category category = loadEditableOrRespond(req, resp, log, "category id=" + id, NOT_FOUND_MESSAGE,
                Routes.ADMIN_CATEGORIES, "categories", () -> service.getForEdit(id));
        if (category != null) {
            renderForm(req, resp, category, true, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Category category = bind(req);
        String idParam = req.getParameter("id");
        boolean isEdit = !Strings.isBlank(idParam);

        if (isEdit) {
            Long id = parseId(idParam);
            if (id == null) {
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            // archived categories are read-only, however this POST was made
            if (loadEditableOrRespond(req, resp, log, "category id=" + id, NOT_FOUND_MESSAGE,
                    Routes.ADMIN_CATEGORIES, "categories", () -> service.getForEdit(id)) == null) {
                return;
            }
            category.setId(id);
        }

        Map<String, String> errors = service.validate(category);
        if (errors.isEmpty()) {
            try {
                errors = isEdit ? service.update(category) : service.create(category);
            } catch (ArchivedEntityException e) {
                log.info("Category id={} was archived before the save", category.getId());
                redirectWithFlash(req, resp, Routes.ADMIN_CATEGORIES, FlashMessages.blockedEdit("categories"));
                return;
            } catch (EntityNotFoundException e) {
                log.warn("Update submitted for missing category id={}", category.getId());
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            } catch (RuntimeException e) {
                log.error("Failed to save category label={}", Log.clean(category.getLabel()), e);
                showServerError(req, resp);
                return;
            }
        }
        if (!errors.isEmpty()) {
            log.info("Category submission rejected ({} field errors): {}", errors.size(), errors.keySet());
            renderForm(req, resp, category, isEdit, errors);
            return;
        }

        log.info("{} category id={} label={}", isEdit ? "Updated" : "Created", category.getId(), Log.clean(category.getLabel()));
        writesDone(req);
        redirectWithFlash(req, resp, Routes.ADMIN_CATEGORIES, isEdit
                ? FlashMessages.updated("Category", category.getLabel())
                : FlashMessages.created("Category", category.getLabel()));
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp, Category category, boolean isEdit,
                            Map<String, String> errors) throws ServletException, IOException {
        CategoryFormPageData data = new CategoryFormPageData(category);
        data.setEdit(isEdit);
        data.setErrors(errors);
        if (data.hasErrors()) {
            showRejected(req, resp, VIEW, data, FlashMessages.validationFailed());
        } else {
            show(req, resp, VIEW, data);
        }
    }

    // this form has no status field - archiving is a separate button
    private static Category bind(HttpServletRequest req) {
        Category category = new Category();
        category.setLabel(trim(req.getParameter("label")));
        category.setEmoji(Strings.trimToNull(req.getParameter("emoji")));
        String colorClass = trim(req.getParameter("colorClass"));
        category.setColorClass(colorClass != null ? colorClass : Category.DEFAULT_COLOR_CLASS);
        return category;
    }
}
