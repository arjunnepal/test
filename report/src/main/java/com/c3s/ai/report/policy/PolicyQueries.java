package com.c3s.ai.report.policy;

/** What the financial report may ask of the allocation tables. The DAO behind it is package-private. */
public interface PolicyQueries {

    static PolicyQueries create() {
        return new JdbcReportPolicyDao();
    }

    ReportPolicy defaultPolicy();
}
