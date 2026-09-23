package com.c3s.ai.browse.donation;

import java.util.List;

/**
 * What the giving page may ask of uwnefl_donations - only donations to published profiles count.
 * The DAO behind it is package-private.
 */
public interface DonationQueries {

    static DonationQueries create() {
        return new JdbcSiteDonationDao();
    }

    /** This week's donations, newest first. */
    List<TickerItem> recentTicker(int limit);

    DonorStats donorStats();
}
