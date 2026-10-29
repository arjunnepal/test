package com.c3s.ai.report.donation;

import java.math.BigDecimal;

/** One profile's donations in a period. The category is its first one; null when it has none. */
public record RecipientTotal(
        String name,
        String categoryLabel,
        String categoryEmoji,
        long transactions,
        long gross,
        BigDecimal charge,
        BigDecimal processing,
        BigDecimal restricted,
        BigDecimal unrestricted) {
}
