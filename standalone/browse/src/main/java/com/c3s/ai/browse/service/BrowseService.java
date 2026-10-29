package com.c3s.ai.browse.service;

import com.c3s.ai.browse.dao.BrowseProfileDao;
import com.c3s.ai.browse.dao.CategoryDao;
import com.c3s.ai.browse.dao.DonationDao;
import com.c3s.ai.browse.dao.JdbcBrowseProfileDao;
import com.c3s.ai.browse.dao.JdbcCategoryDao;
import com.c3s.ai.browse.dao.JdbcDonationDao;
import com.c3s.ai.browse.model.BrowseStats;
import com.c3s.ai.browse.model.BrowseStyle;
import com.c3s.ai.browse.model.Category;
import com.c3s.ai.browse.model.CategoryChip;
import com.c3s.ai.browse.model.CategoryTotal;
import com.c3s.ai.browse.model.Profile;
import com.c3s.ai.browse.model.ProfileCard;
import com.c3s.ai.browse.web.BrowsePageData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowseService {

    private static final int TICKER_LIMIT = 12;
    private static final int TOP_WEEKLY_LIMIT = 5;

    private final BrowseProfileDao profileDao;
    private final DonationDao donationDao;
    private final CategoryDao categoryDao;

    public BrowseService() {
        this(new JdbcBrowseProfileDao(), new JdbcDonationDao(), new JdbcCategoryDao());
    }

    public BrowseService(BrowseProfileDao profileDao, DonationDao donationDao, CategoryDao categoryDao) {
        this.profileDao = profileDao;
        this.donationDao = donationDao;
        this.categoryDao = categoryDao;
    }

    public BrowsePageData buildPage() {
        BrowsePageData data = new BrowsePageData();

        data.setProfiles(buildCards());
        data.setChips(buildChips());
        data.setCategoryTotals(buildCategoryTotals());
        data.setTicker(donationDao.recentTicker(TICKER_LIMIT));

        BrowseProfileDao.PublishedTotals pt = profileDao.publishedTotals();
        DonationDao.DonorStats ds = donationDao.donorStats();
        data.setStats(new BrowseStats(
                pt.count(), pt.raised(),
                ds.activeDonors(), ds.raisedThisWeek(), ds.newDonorsThisMonth()));

        return data;
    }

    private List<ProfileCard> buildCards() {
        List<Profile> published = profileDao.findPublishedWithCategories();
        Map<Long, Long> weeklyByProfile = profileDao.topWeeklyRaised(TOP_WEEKLY_LIMIT);

        List<ProfileCard> cards = new ArrayList<>(published.size());
        for (Profile p : published) {
            Category first = p.getFirstCategory();
            BrowseStyle style = BrowseStyle.forColorClass(first != null ? first.getColorClass() : null);
            List<Long> categoryIds = p.getCategories().stream().map(Category::getId).toList();

            cards.add(new ProfileCard(
                    p.getName(),
                    p.getBlurb(),
                    p.getDisplayEmoji(),
                    nz(p.getGoal()),
                    nz(p.getRaised()),
                    p.getPercentRaised(),
                    first != null ? first.getLabel() : "",
                    style.getTagClass(),
                    style.getCardBackground(),
                    categoryIds,
                    weeklyByProfile.getOrDefault(p.getId(), 0L)));
        }
        // shared by the page cache, so read-only
        return List.copyOf(cards);
    }

    private List<CategoryChip> buildChips() {
        return categoryDao.findInUse().stream()
                .map(c -> new CategoryChip(
                        c.getId(), c.getLabel(), c.getEmoji(),
                        BrowseStyle.forColorClass(c.getColorClass()).getChipClass()))
                .toList();
    }

    private List<CategoryTotal> buildCategoryTotals() {
        List<BrowseProfileDao.CategoryRaised> raw = profileDao.raisedByCategory();
        // bar widths are relative to the biggest category
        long max = raw.stream().mapToLong(BrowseProfileDao.CategoryRaised::raised).max().orElse(0);
        return raw.stream()
                .map(r -> new CategoryTotal(
                        r.label(),
                        r.emoji(),
                        BrowseStyle.forColorClass(r.colorClass()).getAccentColor(),
                        r.raised(),
                        max > 0 ? (int) Math.round(100.0 * r.raised() / max) : 0))
                .toList();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
