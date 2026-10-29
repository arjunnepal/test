package com.c3s.ai.report.donation;

import java.math.BigDecimal;
import java.util.List;

/** Everything the report reads from uwnefl_donations, taken in one snapshot. */
public record DonationReport(
        PeriodTotals yearToDate,
        BigDecimal lastYearCharge,
        List<RecipientTotal> recipients,
        List<DayAmount> days,
        List<CategoryAmount> categories,
        List<TopDonor> topDonors) {

    public DonationReport {
        recipients = List.copyOf(recipients);
        days = List.copyOf(days);
        categories = List.copyOf(categories);
        topDonors = List.copyOf(topDonors);
    }
}
