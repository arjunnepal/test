package com.c3s.ai.admin.allocation;

import java.util.List;

/**
 * Split of a sample donation across the fee and each bucket, for the settings-page preview.
 * double, not BigDecimal - it's a cosmetic preview, not real money.
 */
public record AllocationBreakdown(
        double gift,
        double fee,
        double charge,
        double processing,
        double net,
        FeeModel feeModel,
        List<BucketAmount> buckets) {

    public record BucketAmount(String name, String colorClass, double amount, double pctOfCharge, int pct) {
    }
}
