package com.c3s.ai.browse.page;

import com.c3s.ai.browse.category.CategoryQueries;
import com.c3s.ai.browse.donation.DonationQueries;
import com.c3s.ai.browse.donation.DonorStats;
import com.c3s.ai.browse.profile.CardCategory;
import com.c3s.ai.browse.profile.CategoryRaised;
import com.c3s.ai.browse.profile.ProfileQueries;
import com.c3s.ai.browse.profile.PublishedProfile;
import com.c3s.ai.browse.profile.PublishedTotals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the giving page from each table's read-only query facade. */
public class BrowseService {

    private static final int TICKER_LIMIT = 12;
    private static final int TOP_WEEKLY_LIMIT = 5;

    private final ProfileQueries profiles;
    private final DonationQueries donations;
    private final CategoryQueries categories;

    public BrowseService() {
        this(ProfileQueries.create(), DonationQueries.create(), CategoryQueries.create());
    }

    // tests hand in stub facades
    BrowseService(ProfileQueries profiles, DonationQueries donations, CategoryQueries categories) {
        this.profiles = profiles;
        this.donations = donations;
        this.categories = categories;
    }

    public BrowsePageData buildPage() {
        PublishedTotals totals = profiles.publishedTotals();
        DonorStats donors = donations.donorStats();
        return new BrowsePageData(
                buildCards(),
                buildChips(),
                buildCategoryTotals(),
                donations.recentTicker(TICKER_LIMIT),
                new BrowseStats(totals.count(), totals.raised(),
                        donors.activeDonors(), donors.raisedThisWeek(), donors.newDonorsThisMonth()));
    }

    private List<ProfileCard> buildCards() {
        List<PublishedProfile> published = profiles.findPublished();
        Map<Long, Long> weeklyByProfile = profiles.topWeeklyRaised(TOP_WEEKLY_LIMIT);

        List<ProfileCard> cards = new ArrayList<>(published.size());
        for (PublishedProfile p : published) {
            CardCategory first = p.firstCategory();
            BrowseStyle style = BrowseStyle.forColorClass(first != null ? first.colorClass() : null);
            cards.add(new ProfileCard(
                    p.name(),
                    p.blurb(),
                    p.displayEmoji(),
                    p.goal(),
                    p.raised(),
                    p.percentRaised(),
                    first != null ? first.label() : "",
                    style.getTagClass(),
                    style.getCardBackground(),
                    p.categories().stream().map(CardCategory::id).toList(),
                    weeklyByProfile.getOrDefault(p.id(), 0L)));
        }
        return cards;
    }

    private List<CategoryChip> buildChips() {
        return categories.findInUse().stream()
                .map(c -> new CategoryChip(c.id(), c.label(), c.emoji(),
                        BrowseStyle.forColorClass(c.colorClass()).getChipClass()))
                .toList();
    }

    private List<CategoryTotal> buildCategoryTotals() {
        List<CategoryRaised> raw = profiles.raisedByCategory();
        // bar widths are relative to the biggest category
        long max = raw.stream().mapToLong(CategoryRaised::raised).max().orElse(0);
        return raw.stream()
                .map(r -> new CategoryTotal(
                        r.label(),
                        r.emoji(),
                        BrowseStyle.forColorClass(r.colorClass()).getAccentColor(),
                        r.raised(),
                        max > 0 ? (int) Math.round(100.0 * r.raised() / max) : 0))
                .toList();
    }
}
