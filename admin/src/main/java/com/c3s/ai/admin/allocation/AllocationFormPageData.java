package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.core.web.FormPageData;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Page object for allocation-settings.jsp (+ the allocation-preview.jsp fragment) */
public class AllocationFormPageData extends FormPageData {

    /** Labels are shown only on bar segments at least this wide - mirrored in js/allocation-form.js. */
    public static final double MIN_LABELLED_SEGMENT_PCT = 13;

    /** How many bucket rows the form renders: the stored ones, then spares the Add button reveals. */
    public static final int MAX_BUCKETS = AllocationValidator.MAX_BUCKETS;

    private final CategoryRef scopeCategory;
    private final AllocationSettings settings;
    private final List<AllocationBucket> buckets;
    private final boolean newOverride;
    private final AllocationBreakdown preview;
    private final String sampleName;
    private final DonorMessage donorMessage;
    // one fresh prefix per render, so a revealed spare row can't collide with a stored bucket key
    private final String spareKeyPrefix = "new-" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);

    public AllocationFormPageData(CategoryRef scopeCategory, AllocationSettings settings, List<AllocationBucket> buckets,
                                  boolean newOverride, AllocationBreakdown preview, String sampleName,
                                  DonorMessage donorMessage) {
        this.scopeCategory = scopeCategory;
        this.settings = settings;
        this.buckets = List.copyOf(buckets);
        this.newOverride = newOverride;
        this.preview = preview;
        this.sampleName = sampleName;
        this.donorMessage = donorMessage;
    }

    /** null for the default policy. */
    public CategoryRef getScopeCategory() {
        return scopeCategory;
    }

    public Long getCategoryId() {
        return scopeCategory == null ? null : scopeCategory.id();
    }

    /** A category's first policy, pre-filled from the default. */
    public boolean isNewOverride() {
        return newOverride;
    }

    /** A category's existing policy. */
    public boolean isEditOverride() {
        return scopeCategory != null && !newOverride;
    }

    public AllocationSettings getSettings() {
        return settings;
    }

    public List<AllocationBucket> getBuckets() {
        return buckets;
    }

    public AllocationBreakdown getPreview() {
        return preview;
    }

    /** The processing fee's share of what the donor is charged, as a percent. */
    public double getPreviewFeePct() {
        return preview.charge() > 0 ? preview.processing() / preview.charge() * 100 : 0;
    }

    public String getSampleName() {
        return sampleName;
    }

    /** The donor sentence as values - allocation-preview.jsp renders and escapes them. */
    public DonorMessage getDonorMessage() {
        return donorMessage;
    }

    /** The stored shares added up, for the running total the form shows before any script runs. */
    public int getBucketTotalPct() {
        int total = 0;
        for (AllocationBucket b : buckets) {
            total += b.getPct();
        }
        return total;
    }

    /** Colour slot for the bucket row at this position, filled or still spare. */
    public String getSwatchClass(int index) {
        return AllocationBucket.colorClassFor(index);
    }

    /** The key a spare row submits if the admin reveals it; the server treats it as a new bucket. */
    public String getSpareBucketKey(int index) {
        return spareKeyPrefix + "-" + index;
    }

    /** "0,1,2..." for the hidden bucketIndexes field. */
    public String getBucketIndexesCsv() {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < buckets.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(i);
        }
        return csv.toString();
    }
}
