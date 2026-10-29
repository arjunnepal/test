package com.c3s.ai.report.policy;

import java.math.BigDecimal;

/**
 * The default allocation policy, folded into the report's three-way split: every bucket that goes
 * to the recipient is "restricted", the rest are "unrestricted". It seeds the what-if controls
 * and the fee wording; the report's own figures come from each donation's snapshot.
 */
public record ReportPolicy(FeeModel feeModel, BigDecimal feePct, BigDecimal feeFixed, int unrestrictedPct) {

    /** Used when the default row is missing - the defaults schema.sql creates. */
    public static final ReportPolicy FALLBACK =
            new ReportPolicy(FeeModel.DEDUCTED, new BigDecimal("2.90"), new BigDecimal("0.30"), 15);
}
