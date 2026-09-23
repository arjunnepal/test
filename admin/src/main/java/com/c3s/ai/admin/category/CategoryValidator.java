package com.c3s.ai.admin.category;

import com.c3s.ai.common.util.Strings;

import java.util.LinkedHashMap;
import java.util.Map;

final class CategoryValidator {

    private CategoryValidator() {
    }

    /** Field rules only, no DB. */
    static Map<String, String> validate(Category category) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (Strings.isBlank(category.getLabel())) {
            errors.put("label", "Label is required.");
        } else if (Strings.longerThan(category.getLabel(), Category.LABEL_MAX)) {
            errors.put("label", "Label must be " + Category.LABEL_MAX + " characters or fewer.");
        }

        if (Strings.longerThan(category.getEmoji(), Category.EMOJI_MAX)) {
            errors.put("emoji", "Emoji must be " + Category.EMOJI_MAX + " characters or fewer.");
        }

        if (!Category.COLOR_CLASSES.contains(category.getColorClass())) {
            errors.put("colorClass", "Please choose a color.");
        }

        return errors;
    }
}
