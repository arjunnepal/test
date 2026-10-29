package com.c3s.ai.report.page;

import com.c3s.ai.report.donation.CategoryAmount;
import com.c3s.ai.report.donation.DayAmount;
import com.c3s.ai.report.donation.DonationReport;
import com.c3s.ai.report.donation.DonationQueries;
import com.c3s.ai.report.donation.ReportPeriod;
import com.c3s.ai.report.donation.TopDonor;
import com.c3s.ai.report.policy.PolicyQueries;
import com.c3s.ai.report.policy.ReportPolicy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the financial report from the donation and policy query facades. */
public class ReportService {

    static final int TOP_DONOR_LIMIT = 8;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
    private static final DateTimeFormatter AS_OF = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
    private static final DateTimeFormatter DONOR_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);

    // first place is gold (a class); second and third have the reference's own colours
    private static final String[] RANK_COLORS = {"", "#b0a000", "#a07060"};

    private final DonationQueries donations;
    private final PolicyQueries policies;
    private final Clock clock;

    public ReportService() {
        this(DonationQueries.create(), PolicyQueries.create(), Clock.systemDefaultZone());
    }

    // tests hand in stub facades and a fixed clock
    ReportService(DonationQueries donations, PolicyQueries policies, Clock clock) {
        this.donations = donations;
        this.policies = policies;
        this.clock = clock;
    }

    public ReportPageData buildPage() {
        ReportPeriod period = ReportPeriod.asOf(LocalDate.now(clock));
        ReportPolicy policy = policies.defaultPolicy();
        DonationReport report = donations.load(period, TOP_DONOR_LIMIT);
        return new ReportPageData(labels(period), policy, report.yearToDate(),
                yoyPct(report.yearToDate().charge(), report.lastYearCharge()),
                report.recipients(), weeks(period, report.days()),
                categories(report.categories(), report.yearToDate().gross()),
                donors(report.topDonors()));
    }

    static ReportPageData.Labels labels(ReportPeriod period) {
        LocalDate today = period.today();
        return new ReportPageData.Labels(MONTH.format(today), AS_OF.format(today),
                "January 1 – " + AS_OF.format(today), String.valueOf(today.getYear()));
    }

    static Double yoyPct(BigDecimal thisYear, BigDecimal lastYear) {
        if (lastYear == null || lastYear.signum() <= 0) {
            return null;
        }
        return (thisYear.doubleValue() - lastYear.doubleValue()) / lastYear.doubleValue() * 100;
    }

    /** Twelve Monday-to-Sunday weeks ending with this one; a bar's height is its share of the tallest. */
    static List<WeekBar> weeks(ReportPeriod period, List<DayAmount> days) {
        long[] totals = new long[ReportPeriod.WEEKS];
        for (DayAmount day : days) {
            // checked first: WEEKS.between truncates, so a day just before the start would be week 0
            if (day.day().isBefore(period.weeksStart())) {
                continue;
            }
            long index = ChronoUnit.WEEKS.between(period.weeksStart(), day.day());
            if (index < ReportPeriod.WEEKS) {
                totals[(int) index] += day.amount();
            }
        }
        long max = 0;
        for (long total : totals) {
            max = Math.max(max, total);
        }

        List<WeekBar> bars = new ArrayList<>(ReportPeriod.WEEKS);
        for (int i = 0; i < ReportPeriod.WEEKS; i++) {
            LocalDate start = period.weeksStart().plusWeeks(i);
            boolean current = i == ReportPeriod.WEEKS - 1;
            int height = max > 0 ? (int) Math.round(totals[i] * 100.0 / max) : 0;
            bars.add(new WeekBar(ReportFormat.thousands(totals[i]), height,
                    current ? "This" : "W" + ((start.getDayOfMonth() - 1) / 7 + 1),
                    current ? "wk" : SHORT_MONTH.format(start), current));
        }
        return bars;
    }

    /** Widths are relative to the largest category; shares are of the year's gifts, so they can add past 100%. */
    static List<CategoryRow> categories(List<CategoryAmount> amounts, long yearGross) {
        long max = amounts.stream().mapToLong(CategoryAmount::amount).max().orElse(0);
        List<CategoryRow> rows = new ArrayList<>(amounts.size());
        for (CategoryAmount c : amounts) {
            if (c.amount() <= 0) {
                continue;
            }
            ReportStyle style = ReportStyle.forColorClass(c.colorClass());
            rows.add(new CategoryRow(c.emoji() != null ? c.emoji() : "", c.label(), style.getAccentColor(),
                    (int) Math.round(c.amount() * 100.0 / max), ReportFormat.money0(c.amount()),
                    ReportFormat.pct1(yearGross > 0 ? c.amount() * 100.0 / yearGross : 0)));
        }
        return rows;
    }

    static List<DonorRow> donors(List<TopDonor> top) {
        List<DonorRow> rows = new ArrayList<>(top.size());
        for (int i = 0; i < top.size(); i++) {
            TopDonor d = top.get(i);
            ReportStyle style = ReportStyle.forColorClass(d.colorClass());
            rows.add(new DonorRow(i + 1, i == 0 ? "gold-rank" : "", i < RANK_COLORS.length ? RANK_COLORS[i] : "",
                    d.donorLabel(), d.recipient(), style.getTagClass(),
                    ReportFormat.category(d.categoryEmoji(), d.categoryLabel()), ReportFormat.money0(d.total()),
                    d.frequency().label(), DONOR_DATE.format(d.date())));
        }
        return rows;
    }
}
