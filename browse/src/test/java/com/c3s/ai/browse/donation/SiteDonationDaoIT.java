package com.c3s.ai.browse.donation;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// The ticker and donor stats against the seeded schema. Skipped unless DONATION_IT_JDBC_URL is set.
class SiteDonationDaoIT {

    private final DonationQueries donations = DonationQueries.create();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void tickerShowsThisWeeksDonationsNewestFirst() {
        List<TickerItem> ticker = donations.recentTicker(12);

        assertEquals(12, ticker.size());
        assertEquals(new TickerItem("C. Washington", 2_500, "Sgt. Patricia H."), ticker.get(0));
        assertEquals(new TickerItem("Anonymous", 1_800, "Diane & Ray K."), ticker.get(1));
        assertEquals(new TickerItem("Anonymous", 150, "Sophia L."), ticker.get(11));
        assertEquals(4, ticker.stream().filter(t -> t.donorLabel().equals("Anonymous")).count());
    }

    @Test
    void donorStatsCountEachAnonymousGiftAsItsOwnDonor() {
        DonorStats stats = donations.donorStats();

        // 8 named donors + 4 anonymous gifts
        assertEquals(12, stats.activeDonors());
        assertEquals(9_300, stats.raisedThisWeek());
        // early on the 1st some demo donations are still in last month
        assertTrue(stats.newDonorsThisMonth() >= 0 && stats.newDonorsThisMonth() <= 12);
    }
}
