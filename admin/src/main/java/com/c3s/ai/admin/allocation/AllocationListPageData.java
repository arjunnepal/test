package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.core.web.PageData;

import java.util.List;
import java.util.Set;

/** Page object for allocation-list.jsp */
public class AllocationListPageData extends PageData {

    private final List<CategoryRef> categories;
    private final Set<Long> overrideCategoryIds;

    public AllocationListPageData(List<CategoryRef> categories, Set<Long> overrideCategoryIds) {
        this.categories = List.copyOf(categories);
        this.overrideCategoryIds = Set.copyOf(overrideCategoryIds);
    }

    public List<CategoryRef> getCategories() {
        return categories;
    }

    public boolean hasOverride(long categoryId) {
        return overrideCategoryIds.contains(categoryId);
    }
}
