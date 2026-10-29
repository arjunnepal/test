package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationSettings;

import java.util.List;
import java.util.Set;

// Allocation policy: one default row (category_id NULL) + an optional override per category.
public interface AllocationDao {

    AllocationSettings getDefaultSettings();

    /** null if the category has no override (it uses the default). */
    AllocationSettings getOverride(long categoryId);

    List<AllocationBucket> getBuckets(long settingsId);

    /** Category ids with an override, in one query - for the list page. */
    Set<Long> getOverrideCategoryIds();

    /**
     * Save settings and replace all its buckets in one transaction. Inserts if id is null.
     * Throws EntityNotFoundException if an id was given but the row is gone.
     */
    long saveSettingsAndBuckets(AllocationSettings settings, List<AllocationBucket> buckets)
            throws EntityNotFoundException;
}
