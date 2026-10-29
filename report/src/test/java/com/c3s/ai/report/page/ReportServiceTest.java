package com.c3s.ai.report.page;

import com.c3s.ai.report.donation.CategoryAmount;
import com.c3s.ai.report.donation.DayAmount;
import com.c3s.ai.report.donation.DonationReport;
import com.c3s.ai.report.donation.Frequency;
import com.c3s.ai.report.donation.PeriodTotals;
import com.c3s.ai.report.donation.RecipientTotal;
import com.c3s.ai.report.donation.ReportPeriod;
import com.c3s.ai.report.donation.TopDonor;
import com.c3s.ai.report.policy.ReportPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private static final String GRAD_CAP = new String(Character.toChars(0x1F393));
    private static final String HOUSE = new String(Character.toChars(0x1F3E0));
    private static final String MEDAL = new String(Character.toChars(0x1F396)) + "\uFE0F";

    // a Wednesday
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 13);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Test
    void thePeriodIsTheYearToDateThisMonthAndTwelveWeeksEndingThisOne() {
        ReportPeriod p = ReportPeriod.asOf(TODAY);

        assertEquals(LocalDate.of(2026, 1, 1), p.yearStart());
        assertEquals(LocalDate.of(2026, 5, 14), p.end());
        assertEquals(LocalDate.of(2025, 1, 1), p.lastYearStart());
        assertEquals(LocalDate.of(2025, 5, 14), p.lastYearEnd());
        assertEquals(LocalDate.of(2026, 5, 1), p.monthStart());
        assertEquals(LocalDate.of(2026, 5, 11), p.currentWeekStart());
        assertEquals(LocalDate.of(2026, 2, 23), p.weeksStart());
    }

    @Test
    void daysLandInTheirMondayWeekAndTheCurrentWeekIsLabelledThisWeek() {
        ReportPeriod p = ReportPeriod.asOf(TODAY);
        List<WeekBar> bars = ReportService.weeks(p, List.of(
                new DayAmount(LocalDate.of(2026, 2, 22), 9_999),   // the Sunday before - not charted
                new DayAmount(LocalDate.of(2026, 2, 23), 1_000),
                new DayAmount(LocalDate.of(2026, 3, 1), 1_000),    // same week, Sunday
                new DayAmount(LocalDate.of(2026, 5, 13), 4_000)));

        assertEquals(12, bars.size());
        assertEquals(new WeekBar("$2.0K", 50, "W4", "Feb", false), bars.get(0));
        assertEquals(new WeekBar("$0.0K", 0, "W1", "Mar", false), bars.get(1));
        assertEquals(new WeekBar("$4.0K", 100, "This", "wk", true), bars.get(11));
    }

    @Test
    void anEmptyChartHasTwelveFlatBars() {
        List<WeekBar> bars = ReportService.weeks(ReportPeriod.asOf(TODAY), List.of());

        assertEquals(12, bars.size());
        assertTrue(bars.stream().allMatch(b -> b.heightPx() == 0));
    }

    @Test
    void yearOnYearNeedsSomethingLastYear() {
        assertEquals(25.0, ReportService.yoyPct(new BigDecimal("125"), new BigDecimal("100")), 1e-9);
        assertEquals(-50.0, ReportService.yoyPct(new BigDecimal("50"), new BigDecimal("100")), 1e-9);
        assertNull(ReportService.yoyPct(new BigDecimal("50"), BigDecimal.ZERO));
    }

    @Test
    void categoryBarsAreRelativeToTheLargestAndSharesToTheYear() {
        List<CategoryRow> rows = ReportService.categories(List.of(
                new CategoryAmount("Education", GRAD_CAP, "cat-blue", 2_000),
                new CategoryAmount("Housing", HOUSE, "cat-gold", 500),
                new CategoryAmount("Empty", null, "cat-red", 0)), 4_000);

        assertEquals(2, rows.size(), "a category with nothing given is left out");
        assertEquals(new CategoryRow(GRAD_CAP, "Education", "var(--uw-blue)", 100, "$2,000", "50.0%"), rows.get(0));
        assertEquals(new CategoryRow(HOUSE, "Housing", "#92620a", 25, "$500", "12.5%"), rows.get(1));
    }

    @Test
    void theTopThreeDonorsGetTheReferencesRankColours() {
        TopDonor d = new TopDonor("C. Washington", 2_500, "Sgt. Patricia H.", "Veterans", MEDAL, "cat-purple",
                Frequency.MONTHLY, LocalDate.of(2026, 5, 1));
        List<DonorRow> rows = ReportService.donors(List.of(d, d, d, d));

        assertEquals(new DonorRow(1, "gold-rank", "", "C. Washington", "Sgt. Patricia H.", "tv",
                MEDAL + " Veterans", "$2,500", "Monthly", "May 1"), rows.get(0));
        assertEquals("#b0a000", rows.get(1).rankColor());
        assertEquals("#a07060", rows.get(2).rankColor());
        assertEquals("", rows.get(3).rankClass());
        assertEquals("", rows.get(3).rankColor());
    }

    @Test
    void buildPageAsksForTheClocksPeriodAndCarriesThePolicy() {
        AtomicReference<ReportPeriod> asked = new AtomicReference<>();
        PeriodTotals totals = new PeriodTotals(2, 150, new BigDecimal("150.00"), new BigDecimal("4.95"),
                new BigDecimal("4.95"), new BigDecimal("123.29"), new BigDecimal("21.76"));
        RecipientTotal recipient = new RecipientTotal("Marcus T.", "Education", GRAD_CAP, 2, 150,
                new BigDecimal("150.00"), new BigDecimal("4.95"), new BigDecimal("123.29"), new BigDecimal("21.76"));
        ReportService service = new ReportService((period, limit) -> {
            asked.set(period);
            assertEquals(ReportService.TOP_DONOR_LIMIT, limit);
            return new DonationReport(totals, new BigDecimal("100"), List.of(recipient), List.of(), List.of(), List.of());
        }, () -> ReportPolicy.FALLBACK, CLOCK);

        ReportPageData page = service.buildPage();

        assertEquals(TODAY, asked.get().today());
        assertEquals(new ReportPageData.Labels("May 2026", "May 13, 2026", "January 1 – May 13, 2026", "2026"),
                page.getLabels());
        assertEquals(ReportPolicy.FALLBACK, page.getPolicy());
        assertEquals(totals, page.getTotals());
        assertEquals(50.0, page.getYoyPct(), 1e-9);
        assertEquals(List.of(recipient), page.getRecipients());
        assertEquals(12, page.getWeeks().size());
        assertFalse(page.getWeeks().get(0).current());
    }
}
