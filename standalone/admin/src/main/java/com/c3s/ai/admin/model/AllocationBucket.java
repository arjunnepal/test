package com.c3s.ai.admin.model;

/**
 * One destination for a share of the net gift. The two core buckets (restricted /
 * unrestricted) can't be removed; admins can add more.
 */
public class AllocationBucket {

    // swatch slot per bucket position; also used as CSS class suffixes and mirrored in
    // allocation-preview.js / allocation-form.js - do not rename the values.
    private static final String[] SWATCH_CLASSES = {"res", "unres", "c3", "c4", "c5", "c6"};

    private Long id;
    private String bucketKey;
    private String name;
    private int pct;
    private boolean toRecipient;
    private boolean core;
    private String description;
    private String purpose;
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPct() {
        return pct;
    }

    public void setPct(int pct) {
        this.pct = pct;
    }

    public boolean isToRecipient() {
        return toRecipient;
    }

    public void setToRecipient(boolean toRecipient) {
        this.toRecipient = toRecipient;
    }

    public boolean isCore() {
        return core;
    }

    public void setCore(boolean core) {
        this.core = core;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** Swatch color slot, cycles past the two core buckets. */
    public String getColorClass() {
        return SWATCH_CLASSES[Math.floorMod(sortOrder, SWATCH_CLASSES.length)];
    }

    /** purpose, else description, else name (null for the recipient bucket). */
    public String getEffectivePurpose() {
        if (purpose != null && !purpose.isBlank()) {
            return purpose;
        }
        if (toRecipient) {
            return null;
        }
        if (description != null && !description.isBlank()) {
            return description;
        }
        return name;
    }
}
