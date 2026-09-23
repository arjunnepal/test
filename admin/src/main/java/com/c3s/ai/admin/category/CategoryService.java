package com.c3s.ai.admin.category;

import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.ConstraintViolationException;
import com.c3s.ai.common.exception.EntityNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * Category use cases and their rules, which the package-private DAO makes unskippable: archived
 * categories can't be edited, and a duplicate label is a field error rather than a 500.
 */
public class CategoryService {

    static final String DUPLICATE_LABEL = "This label is already in use by another category.";

    private final CategoryDao dao;

    public CategoryService() {
        this(new JdbcCategoryDao());
    }

    CategoryService(CategoryDao dao) {
        this.dao = dao;
    }

    public List<Category> listAll() {
        return dao.findAll();
    }

    /** Active categories by label, for pickers and lists elsewhere in the dashboard. */
    public List<CategoryRef> activeOptions() {
        return dao.findActiveRefs();
    }

    /** An active category, or not found - an archived one can't take a new allocation policy. */
    public CategoryRef getActive(long id) throws EntityNotFoundException {
        return dao.findActiveRefs().stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("category", id));
    }

    /** The category for its edit form. */
    public Category getForEdit(long id) throws EntityNotFoundException, ArchivedEntityException {
        Category category = dao.findById(id).orElseThrow(() -> new EntityNotFoundException("category", id));
        if (!category.isActive()) {
            throw new ArchivedEntityException("category", id);
        }
        return category;
    }

    /** Field rules plus label uniqueness (one query). Empty when valid. */
    public Map<String, String> validate(Category category) {
        Map<String, String> errors = CategoryValidator.validate(category);
        if (!errors.containsKey("label") && dao.labelExists(category.getLabel().trim(), category.getId())) {
            errors.put("label", DUPLICATE_LABEL);
        }
        return errors;
    }

    /** Inserts an active category and sets its id. Returns field errors instead when the database refuses. */
    public Map<String, String> create(Category category) {
        category.setStatus(CategoryStatus.ACTIVE);
        try {
            category.setId(dao.insert(category));
            return Map.of();
        } catch (ConstraintViolationException e) {
            return duplicateOrRethrow(e);
        }
    }

    /** Updates label, emoji and colour. Returns field errors instead when the database refuses. */
    public Map<String, String> update(Category category) throws EntityNotFoundException, ArchivedEntityException {
        long id = category.getId();
        boolean updated;
        try {
            updated = dao.updateActive(category);
        } catch (ConstraintViolationException e) {
            return duplicateOrRethrow(e);
        }
        if (!updated) {
            // no active row matched: tell apart "gone" from "archived meanwhile"
            Category current = dao.findById(id).orElseThrow(() -> new EntityNotFoundException("category", id));
            if (!current.isActive()) {
                throw new ArchivedEntityException("category", id);
            }
        }
        return Map.of();
    }

    /** Soft-delete; returns the label for the confirmation message. */
    public String archive(long id) throws EntityNotFoundException {
        String label = dao.findLabel(id).orElseThrow(() -> new EntityNotFoundException("category", id));
        if (!dao.archive(id)) {
            throw new EntityNotFoundException("category", id);
        }
        return label;
    }

    private static Map<String, String> duplicateOrRethrow(ConstraintViolationException e) {
        if (e.involves("label")) {
            return Map.of("label", DUPLICATE_LABEL);
        }
        throw e;
    }
}
