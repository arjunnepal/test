package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.Category;

import java.util.List;

// page object for categories-list.jsp
public class CategoryListPageData extends PageData {

    private List<Category> categories;

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
