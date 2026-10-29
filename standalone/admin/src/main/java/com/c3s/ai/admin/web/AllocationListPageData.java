package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.Category;

import java.util.List;
import java.util.Map;

// page object for allocation-list.jsp
public class AllocationListPageData extends PageData {

    private List<Category> categories;
    private Map<Long, Boolean> categoryHasOverride;

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public Map<Long, Boolean> getCategoryHasOverride() {
        return categoryHasOverride;
    }

    public void setCategoryHasOverride(Map<Long, Boolean> categoryHasOverride) {
        this.categoryHasOverride = categoryHasOverride;
    }

    public boolean hasOverride(Long categoryId) {
        return categoryHasOverride != null && Boolean.TRUE.equals(categoryHasOverride.get(categoryId));
    }
}
