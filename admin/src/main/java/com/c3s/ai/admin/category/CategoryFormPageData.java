package com.c3s.ai.admin.category;

import com.c3s.ai.core.web.FormPageData;

import java.util.List;
import java.util.Locale;

/** Page object for category-form.jsp */
public class CategoryFormPageData extends FormPageData {

    /** one <option> in the color select: cat-* class + display label */
    public record ColorOption(String value, String label) {
    }

    private static final List<ColorOption> COLOR_OPTIONS = Category.COLOR_CLASSES.stream()
            .map(cc -> {
                String name = cc.replace("cat-", "");
                return new ColorOption(cc, name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
            })
            .toList();

    private final Category category;

    public CategoryFormPageData(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public List<ColorOption> getColorOptions() {
        return COLOR_OPTIONS;
    }
}
