package com.c3s.ai.admin.category;

import com.c3s.ai.core.web.PageData;

import java.util.List;

/** Page object for categories-list.jsp */
public class CategoryListPageData extends PageData {

    private final List<Category> categories;

    public CategoryListPageData(List<Category> categories) {
        this.categories = List.copyOf(categories);
    }

    public List<Category> getCategories() {
        return categories;
    }
}
