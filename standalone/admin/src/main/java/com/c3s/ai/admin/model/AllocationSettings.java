package com.c3s.ai.admin.model;

import java.math.BigDecimal;

/** An allocation policy - the default (categoryId null) or a category override. */
public class AllocationSettings {

    /** Starting fee for a brand-new policy (also the DB column default in schema.sql). */
    public static final BigDecimal DEFAULT_FEE_PCT = new BigDecimal("2.90");
    public static final BigDecimal DEFAULT_FEE_FIXED = new BigDecimal("0.30");

    private Long id;
    private Long categoryId;
    private FeeModel feeModel = FeeModel.DEDUCTED;
    private BigDecimal feePct = DEFAULT_FEE_PCT;
    private BigDecimal feeFixed = DEFAULT_FEE_FIXED;
    private boolean allowDonorAdjust;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public FeeModel getFeeModel() {
        return feeModel;
    }

    public void setFeeModel(FeeModel feeModel) {
        this.feeModel = feeModel;
    }

    public BigDecimal getFeePct() {
        return feePct;
    }

    public void setFeePct(BigDecimal feePct) {
        this.feePct = feePct;
    }

    public BigDecimal getFeeFixed() {
        return feeFixed;
    }

    public void setFeeFixed(BigDecimal feeFixed) {
        this.feeFixed = feeFixed;
    }

    public boolean isAllowDonorAdjust() {
        return allowDonorAdjust;
    }

    public void setAllowDonorAdjust(boolean allowDonorAdjust) {
        this.allowDonorAdjust = allowDonorAdjust;
    }
}
