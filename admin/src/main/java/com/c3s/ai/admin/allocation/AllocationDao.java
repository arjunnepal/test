package com.c3s.ai.admin.allocation;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/** Package-private: allocation policies are only read and saved through AllocationService. */
interface AllocationDao {

    Optional<AllocationSettings> findDefault();

    Optional<AllocationSettings> findOverride(long categoryId);

    List<AllocationBucket> findBuckets(long settingsId);

    /** Category ids with an override, in one query - for the list page. */
    Set<Long> findOverrideCategoryIds();

    /** Settings and buckets in one transaction; inserts when id is null, empty when the row is gone. */
    OptionalLong saveSettingsAndBuckets(AllocationSettings settings, List<AllocationBucket> buckets);
}
