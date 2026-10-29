package com.c3s.ai.admin.service;

import com.c3s.ai.admin.dao.AllocationDao;
import com.c3s.ai.admin.dao.CategoryDao;
import com.c3s.ai.admin.dao.JdbcAllocationDao;
import com.c3s.ai.admin.dao.JdbcCategoryDao;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationSettings;
import com.c3s.ai.admin.model.Category;

import java.util.List;
import java.util.Set;

/**
 * Allocation-policy use cases. Security: the target settings row is always resolved
 * server-side from the category (resolveTarget), never from a client-supplied id.
 */
public class AllocationService {

    private final AllocationDao allocationDao;
    private final CategoryDao categoryDao;

    public AllocationService() {
        this(new JdbcAllocationDao(), new JdbcCategoryDao());
    }

    public AllocationService(AllocationDao allocationDao, CategoryDao categoryDao) {
        this.allocationDao = allocationDao;
        this.categoryDao = categoryDao;
    }

    public AllocationSettings defaultSettings() {
        return allocationDao.getDefaultSettings();
    }

    public AllocationSettings override(long categoryId) {
        return allocationDao.getOverride(categoryId);
    }

    public List<AllocationBucket> buckets(long settingsId) {
        return allocationDao.getBuckets(settingsId);
    }

    public Set<Long> overrideCategoryIds() {
        return allocationDao.getOverrideCategoryIds();
    }

    public List<Category> activeCategories() {
        return categoryDao.findAll(true);
    }

    public Category get(long id) throws EntityNotFoundException {
        return categoryDao.findById(id);
    }

    /** Which settings row a save hits: default, an existing override, or a fresh insert (settingsId null). */
    public record Target(Long settingsId, boolean isNewOverride) {
    }

    public Target resolveTarget(Long categoryId) {
        if (categoryId == null) {
            return new Target(defaultSettings().getId(), false);
        }
        AllocationSettings existing = allocationDao.getOverride(categoryId);
        return new Target(existing != null ? existing.getId() : null, existing == null);
    }

    public long save(AllocationSettings settings, List<AllocationBucket> buckets) throws EntityNotFoundException {
        return allocationDao.saveSettingsAndBuckets(settings, buckets);
    }
}
