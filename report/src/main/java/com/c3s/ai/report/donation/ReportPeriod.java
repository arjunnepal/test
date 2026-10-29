package com.c3s.ai.report.donation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * The date ranges one report covers, all half-open [from, to) on donated_at. Built from "today"
 * so the service can be tested against a fixed clock.
 */
public record ReportPeriod(
        LocalDate today,
        LocalDate yearStart,
        LocalDate end,
        LocalDate lastYearStart,
        LocalDate lastYearEnd,
        LocalDate monthStart,
        LocalDate weeksStart) {

    /** Bars on the weekly chart, the current week last. */
    public static final int WEEKS = 12;

    public static ReportPeriod asOf(LocalDate today) {
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate end = today.plusDays(1);
        LocalDate currentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new ReportPeriod(today, yearStart, end,
                yearStart.minusYears(1), end.minusYears(1),
                today.withDayOfMonth(1),
                currentWeek.minusWeeks(WEEKS - 1));
    }

    /** Monday of the current week. */
    public LocalDate currentWeekStart() {
        return weeksStart.plusWeeks(WEEKS - 1);
    }
}
