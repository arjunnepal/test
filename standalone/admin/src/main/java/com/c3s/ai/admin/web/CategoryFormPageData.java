package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// page object for category-form.jsp
public class CategoryFormPageData extends FormPageData {

    /** one <option> in the color select: cat-* class + display label */
    public record ColorOption(String value, String label) {
    }

    private boolean edit;
    private Category category;
    private List<ColorOption> colorOptions = List.of();

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /** Builds the <option> list once - the JSP just iterates it. */
    public void setColorClasses(List<String> colorClasses) {
        List<ColorOption> options = new ArrayList<>();
        for (String cc : colorClasses) {
            String name = cc.replace("cat-", "");
            String label = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
            options.add(new ColorOption(cc, label));
        }
        this.colorOptions = List.copyOf(options);
    }

    public List<ColorOption> getColorOptions() {
        return colorOptions;
    }
}
