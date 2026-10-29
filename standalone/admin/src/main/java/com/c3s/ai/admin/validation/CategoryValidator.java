package com.c3s.ai.admin.validation;

import com.c3s.ai.admin.dao.CategoryDao;
import com.c3s.ai.admin.model.Category;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CategoryValidator {

    private static final int LABEL_MAX = 100;

    private CategoryValidator() {
    }

    /** Field rules only, no DB. */
    public static Map<String, String> validate(Category category) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(category.getLabel())) {
            errors.put("label", "Label is required.");
        } else if (category.getLabel().length() > LABEL_MAX) {
            errors.put("label", "Label must be " + LABEL_MAX + " characters or fewer.");
        }

        if (!Category.COLOR_CLASSES.contains(category.getColorClass())) {
            errors.put("colorClass", "Please choose a color.");
        }

        return errors;
    }

    // Field rules + a label-uniqueness query, so a dup shows up as a normal field error
    // instead of a duplicate-key exception. The UNIQUE constraint still backstops the race.
    public static Map<String, String> validate(Category category, CategoryDao categoryDao) {
        Map<String, String> errors = validate(category);

        if (errors.get("label") == null && !isBlank(category.getLabel())
                && categoryDao.labelExists(category.getLabel(), category.getId())) {
            errors.put("label", "This label is already in use by another category.");
        }

        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
