package com.c3s.ai.browse.model;

public record BrowseStats(
        long livesSupported,
        long totalRaised,
        long activeDonors,
        long raisedThisWeek,
        long newDonorsThisMonth) {

    public static final BrowseStats EMPTY = new BrowseStats(0, 0, 0, 0, 0);
}
