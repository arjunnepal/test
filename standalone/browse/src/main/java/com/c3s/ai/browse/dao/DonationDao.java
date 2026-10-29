package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.model.TickerItem;

import java.util.List;

public interface DonationDao {

    List<TickerItem> recentTicker(int limit);

    DonorStats donorStats();

    record DonorStats(long activeDonors, long raisedThisWeek, long newDonorsThisMonth) {
    }
}
