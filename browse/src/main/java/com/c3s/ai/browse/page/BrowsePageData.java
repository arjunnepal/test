package com.c3s.ai.browse.page;

import com.c3s.ai.browse.donation.TickerItem;
import com.c3s.ai.core.web.PageData;

import java.util.List;

/**
 * Everything the giving page shows. Built once and shared by every request through the page
 * cache, so it's immutable: the lists are copies and the message can't be set.
 */
public final class BrowsePageData extends PageData {

    private final List<ProfileCard> profiles;
    private final List<CategoryChip> chips;
    private final List<CategoryTotal> categoryTotals;
    private final List<TickerItem> ticker;
    private final BrowseStats stats;

    public BrowsePageData(List<ProfileCard> profiles, List<CategoryChip> chips, List<CategoryTotal> categoryTotals,
                          List<TickerItem> ticker, BrowseStats stats) {
        this.profiles = List.copyOf(profiles);
        this.chips = List.copyOf(chips);
        this.categoryTotals = List.copyOf(categoryTotals);
        this.ticker = List.copyOf(ticker);
        this.stats = stats != null ? stats : BrowseStats.EMPTY;
    }

    public List<ProfileCard> getProfiles() {
        return profiles;
    }

    public List<CategoryChip> getChips() {
        return chips;
    }

    public List<CategoryTotal> getCategoryTotals() {
        return categoryTotals;
    }

    public List<TickerItem> getTicker() {
        return ticker;
    }

    public BrowseStats getStats() {
        return stats;
    }

    @Override
    public void setMessage(String message) {
        throw new UnsupportedOperationException("BrowsePageData is shared by every request and can't carry a message");
    }
}
