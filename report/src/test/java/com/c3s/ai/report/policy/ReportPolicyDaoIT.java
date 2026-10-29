package com.c3s.ai.report.policy;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The default policy from the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
class ReportPolicyDaoIT {

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void theDefaultPolicyFoldsItsBucketsIntoTheUnrestrictedShare() {
        ReportPolicy policy = PolicyQueries.create().defaultPolicy();

        assertEquals(FeeModel.DEDUCTED, policy.feeModel());
        assertEquals(0, new BigDecimal("2.90").compareTo(policy.feePct()));
        assertEquals(0, new BigDecimal("0.30").compareTo(policy.feeFixed()));
        assertEquals(15, policy.unrestrictedPct());
    }
}
