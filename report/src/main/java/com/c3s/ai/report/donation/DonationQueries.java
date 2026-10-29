package com.c3s.ai.report.donation;

/**
 * What the financial report may ask of uwnefl_donations. Every donation counts, whatever its
 * profile's status now - the money was collected. The DAO behind it is package-private.
 */
public interface DonationQueries {

    static DonationQueries create() {
        return new JdbcReportDonationDao();
    }

    /** Year to date, last year's same span, the weekly chart's days and this month's top donors. */
    DonationReport load(ReportPeriod period, int topDonorLimit);
}
