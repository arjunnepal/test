package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.model.Profile;

import java.util.List;
import java.util.Map;

public interface BrowseProfileDao {

    List<Profile> findPublishedWithCategories();

    PublishedTotals publishedTotals();

    List<CategoryRaised> raisedByCategory();

    // profiles with the most donations in the last 7 days: profile id -> total
    Map<Long, Long> topWeeklyRaised(int limit);

    record PublishedTotals(long count, long raised) {
    }

    record CategoryRaised(String label, String emoji, String colorClass, long raised) {
    }
}
