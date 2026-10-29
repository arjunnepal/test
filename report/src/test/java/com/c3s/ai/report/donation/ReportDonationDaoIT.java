package com.c3s.ai.report.donation;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The report's queries against the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
class ReportDonationDaoIT {

    private static DonationReport report;

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
        report = DonationQueries.create().load(ReportPeriod.asOf(LocalDate.now()), 8);
    }

    @Test
    void theSplitAddsBackUpToWhatWasCollected() {
        PeriodTotals t = report.yearToDate();

        assertTrue(t.transactions() >= 12, "the twelve seeded gifts from this week, at least");
        assertEquals(0, t.charge().compareTo(t.processing().add(t.restricted()).add(t.unrestricted())));
        // the seeded policy deducts the fee, so the donor paid exactly the gift
        assertEquals(0, t.charge().compareTo(BigDecimal.valueOf(t.gross())));
    }

    @Test
    void theRecipientRowsAddUpToTheTotals() {
        long tx = 0;
        long gross = 0;
        BigDecimal charge = BigDecimal.ZERO;
        for (RecipientTotal r : report.recipients()) {
            tx += r.transactions();
            gross += r.gross();
            charge = charge.add(r.charge());
            assertEquals(0, r.charge().compareTo(r.processing().add(r.restricted()).add(r.unrestricted())), r.name());
        }
        assertEquals(report.yearToDate().transactions(), tx);
        assertEquals(report.yearToDate().gross(), gross);
        assertEquals(0, report.yearToDate().charge().compareTo(charge));
    }

    @Test
    void theSeededHistoryGivesLastYearSomethingToCompareWith() {
        assertTrue(report.lastYearCharge().signum() > 0);
    }

    @Test
    void topDonorsAreRankedByTotalAndEachAnonymousGiftStandsAlone() {
        assertFalse(report.topDonors().isEmpty());
        assertTrue(report.topDonors().size() <= 8);
        for (int i = 1; i < report.topDonors().size(); i++) {
            assertTrue(report.topDonors().get(i - 1).total() >= report.topDonors().get(i).total());
        }
        // the largest anonymous gift this week is $1,800 and no other anonymous gift joins it
        report.topDonors().stream().filter(d -> d.donorLabel().equals("Anonymous")).findFirst()
                .ifPresent(d -> assertEquals(1_800, d.total()));
    }

    @Test
    void theChartsOnlyCountTheirOwnPeriod() {
        ReportPeriod period = ReportPeriod.asOf(LocalDate.now());
        assertTrue(report.days().stream().noneMatch(d -> d.day().isBefore(period.weeksStart())));
        assertFalse(report.categories().isEmpty());
        for (int i = 1; i < report.categories().size(); i++) {
            assertTrue(report.categories().get(i - 1).amount() >= report.categories().get(i).amount());
        }
    }
}
