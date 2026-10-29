package com.c3s.ai.report.page;

import com.c3s.ai.core.web.PageData;
import com.c3s.ai.report.donation.PeriodTotals;
import com.c3s.ai.report.donation.RecipientTotal;
import com.c3s.ai.report.policy.ReportPolicy;

import java.util.List;

/**
 * Everything the financial report shows. Money stays numeric (ReportFormat prints it) because the
 * JSP also hands the raw figures to js/report.js for the what-if controls.
 */
public final class ReportPageData extends PageData {

    private final Labels labels;
    private final ReportPolicy policy;
    private final PeriodTotals totals;
    // change in total collected against the same span last year; null when there's nothing to compare
    private final Double yoyPct;
    private final List<RecipientTotal> recipients;
    private final List<WeekBar> weeks;
    private final List<CategoryRow> categories;
    private final List<DonorRow> donors;

    /** The dates as the page prints them. */
    public record Labels(String month, String asOf, String period, String year) {
    }

    public ReportPageData(Labels labels, ReportPolicy policy, PeriodTotals totals, Double yoyPct,
                          List<RecipientTotal> recipients, List<WeekBar> weeks, List<CategoryRow> categories,
                          List<DonorRow> donors) {
        this.labels = labels;
        this.policy = policy;
        this.totals = totals;
        this.yoyPct = yoyPct;
        this.recipients = List.copyOf(recipients);
        this.weeks = List.copyOf(weeks);
        this.categories = List.copyOf(categories);
        this.donors = List.copyOf(donors);
    }

    public Labels getLabels() {
        return labels;
    }

    public ReportPolicy getPolicy() {
        return policy;
    }

    public PeriodTotals getTotals() {
        return totals;
    }

    public Double getYoyPct() {
        return yoyPct;
    }

    public List<RecipientTotal> getRecipients() {
        return recipients;
    }

    public List<WeekBar> getWeeks() {
        return weeks;
    }

    public List<CategoryRow> getCategories() {
        return categories;
    }

    public List<DonorRow> getDonors() {
        return donors;
    }
}
