package com.c3s.ai.browse.web;

import com.c3s.ai.browse.model.BrowseStats;
import com.c3s.ai.browse.model.CategoryChip;
import com.c3s.ai.browse.model.CategoryTotal;
import com.c3s.ai.browse.model.ProfileCard;
import com.c3s.ai.browse.model.TickerItem;

import java.util.List;

public class BrowsePageData extends PageData {

    private List<ProfileCard> profiles = List.of();
    private List<CategoryChip> chips = List.of();
    private List<CategoryTotal> categoryTotals = List.of();
    private List<TickerItem> ticker = List.of();
    private BrowseStats stats = BrowseStats.EMPTY;

    public List<ProfileCard> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ProfileCard> profiles) {
        this.profiles = profiles != null ? profiles : List.of();
    }

    public List<CategoryChip> getChips() {
        return chips;
    }

    public void setChips(List<CategoryChip> chips) {
        this.chips = chips != null ? chips : List.of();
    }

    public List<CategoryTotal> getCategoryTotals() {
        return categoryTotals;
    }

    public void setCategoryTotals(List<CategoryTotal> categoryTotals) {
        this.categoryTotals = categoryTotals != null ? categoryTotals : List.of();
    }

    public List<TickerItem> getTicker() {
        return ticker;
    }

    public void setTicker(List<TickerItem> ticker) {
        this.ticker = ticker != null ? ticker : List.of();
    }

    public BrowseStats getStats() {
        return stats;
    }

    public void setStats(BrowseStats stats) {
        this.stats = stats != null ? stats : BrowseStats.EMPTY;
    }
}
