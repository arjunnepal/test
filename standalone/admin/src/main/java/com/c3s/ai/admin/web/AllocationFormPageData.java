package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.AllocationBreakdown;
import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationSettings;
import com.c3s.ai.admin.model.Category;

import java.util.List;

// page object for allocation-settings.jsp (+ the allocation-preview.jsp fragment)
public class AllocationFormPageData extends FormPageData {

    private String action;   // "default" | "new" (first override for a category) | "edit"
    private Long categoryId;
    private Category scopeCategory;
    private AllocationSettings settings;
    private List<AllocationBucket> buckets;
    private AllocationBreakdown preview;
    private String sampleName;
    private String donorMessage;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Category getScopeCategory() {
        return scopeCategory;
    }

    public void setScopeCategory(Category scopeCategory) {
        this.scopeCategory = scopeCategory;
    }

    public AllocationSettings getSettings() {
        return settings;
    }

    public void setSettings(AllocationSettings settings) {
        this.settings = settings;
    }

    public List<AllocationBucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<AllocationBucket> buckets) {
        this.buckets = buckets;
    }

    public AllocationBreakdown getPreview() {
        return preview;
    }

    public void setPreview(AllocationBreakdown preview) {
        this.preview = preview;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getDonorMessage() {
        return donorMessage;
    }

    public void setDonorMessage(String donorMessage) {
        this.donorMessage = donorMessage;
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
