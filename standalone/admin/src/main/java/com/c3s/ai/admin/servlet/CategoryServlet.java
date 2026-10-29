package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.CategoryStatus;
import com.c3s.ai.admin.service.CategoryService;
import com.c3s.ai.admin.util.FlashMessages;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.CategoryFormPageData;
import com.c3s.ai.admin.web.FlashScope;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

// GET /category [?id=X] - add/edit form. POST /category - validate and save.
@WebServlet(name = "CategoryServlet", urlPatterns = "/category")
public class CategoryServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(CategoryServlet.class);
    private static final String FORM_VIEW = "/WEB-INF/views/category-form.jsp";
    private static final String NOT_FOUND_MESSAGE = "That category could not be found.";

    private final CategoryService service = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Category category;
        boolean isEdit;

        if (idParam == null || idParam.isBlank()) {
            category = new Category();
            isEdit = false;
        } else {
            Long id = parseId(idParam);
            if (id == null) {
                log.warn("Edit requested for malformed category id={}", Log.clean(idParam));
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            category = loadOrRespond(req, resp, log, "category id=" + id, NOT_FOUND_MESSAGE,
                    () -> service.get(id));
            if (category == null) {
                return;
            }
            if (!category.isActive()) {
                // block editing an archived category even by a hand-typed URL, not just via the list
                log.info("Blocked edit attempt on archived category id={}", id);
                FlashScope.push(req, FlashMessages.blockedEdit("categories"));
                resp.sendRedirect(req.getContextPath() + "/categories");
                return;
            }
            isEdit = true;
        }

        renderForm(req, resp, category, isEdit, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        Category category = bindCategory(req);
        String idParam = req.getParameter("id");
        boolean isEdit = idParam != null && !idParam.isBlank();

        // this form has no status field (archiving is a separate button), so keep
        // whatever status is already stored instead of resetting to ACTIVE on every save
        if (isEdit) {
            Long id = parseId(idParam);
            if (id == null) {
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            category.setId(id);
            Category existing = loadOrRespond(req, resp, log, "category id=" + id, NOT_FOUND_MESSAGE,
                    () -> service.get(id));
            if (existing == null) {
                return;
            }
            category.setStatus(existing.getStatus());
        }

        Map<String, String> errors = service.validate(category);

        if (!errors.isEmpty()) {
            log.info("Category submission rejected ({} field errors): {}", errors.size(), errors.keySet());
            renderForm(req, resp, category, isEdit, errors);
            return;
        }

        try {
            if (isEdit) {
                service.update(category);
                log.info("Updated category id={} label={}", category.getId(), Log.clean(category.getLabel()));
            } else {
                long id = service.create(category);
                category.setId(id);
                log.info("Created category id={} label={}", id, Log.clean(category.getLabel()));
            }
        } catch (EntityNotFoundException e) {
            log.warn("Update submitted for missing category id={}", Log.clean(idParam));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        } catch (RuntimeException e) {
            log.error("Failed to save category label={}", Log.clean(category.getLabel()), e);
            showServerError(req, resp);
            return;
        }

        String flash = isEdit
                ? FlashMessages.updated("Category", category.getLabel())
                : FlashMessages.created("Category", category.getLabel());
        FlashScope.push(req, flash);
        resp.sendRedirect(req.getContextPath() + "/categories");
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp,
                            Category category, boolean isEdit, Map<String, String> errors)
            throws ServletException, IOException {
        CategoryFormPageData data = new CategoryFormPageData();
        data.setEdit(isEdit);
        data.setCategory(category);
        data.setColorClasses(Category.COLOR_CLASSES);
        data.setErrors(errors);
        if (errors != null && !errors.isEmpty()) {
            renderRejected(req, resp, FORM_VIEW, data, FlashMessages.validationFailed());
        } else {
            render(req, resp, FORM_VIEW, data);
        }
    }

    private Category bindCategory(HttpServletRequest req) {
        Category category = new Category();
        category.setLabel(trim(req.getParameter("label")));
        category.setEmoji(trim(req.getParameter("emoji")));
        String colorClass = trim(req.getParameter("colorClass"));
        category.setColorClass(colorClass != null ? colorClass : Category.DEFAULT_COLOR_CLASS);
        category.setStatus(CategoryStatus.ACTIVE);
        return category;
    }
}
