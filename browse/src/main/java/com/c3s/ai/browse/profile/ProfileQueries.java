package com.c3s.ai.browse.profile;

import java.util.List;
import java.util.Map;

/**
 * What the giving page may ask of uwnefl_profiles: published rows, read-only, and only the columns
 * a card or a stat shows. The DAO behind it is package-private, so nothing can widen the query.
 */
public interface ProfileQueries {

    static ProfileQueries create() {
        return new JdbcSiteProfileDao();
    }

    /** Published profiles, oldest first ("Featured"), each with its active categories. */
    List<PublishedProfile> findPublished();

    PublishedTotals publishedTotals();

    /** Active categories that have published profiles, in creation order. */
    List<CategoryRaised> raisedByCategory();

    /** Profiles with the most donated in the last 7 days: profile id -> total. */
    Map<Long, Long> topWeeklyRaised(int limit);
}
