package com.c3s.ai.report.donation;

import java.time.LocalDate;

/**
 * A donor's total for a period. Recipient, category, frequency and date are those of their
 * largest gift. Each anonymous gift is its own donor.
 */
public record TopDonor(
        String donorLabel,
        long total,
        String recipient,
        String categoryLabel,
        String categoryEmoji,
        String colorClass,
        Frequency frequency,
        LocalDate date) {
}
