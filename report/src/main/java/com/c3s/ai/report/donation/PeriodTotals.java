package com.c3s.ai.report.donation;

import java.math.BigDecimal;

/** Sums of the settlement snapshot over a period. gross is the gifts themselves (amount). */
public record PeriodTotals(
        long transactions,
        long gross,
        BigDecimal charge,
        BigDecimal fee,
        BigDecimal processing,
        BigDecimal restricted,
        BigDecimal unrestricted) {

    public static final PeriodTotals EMPTY = new PeriodTotals(0, 0,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
}
