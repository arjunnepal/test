package com.c3s.ai.report.page;

import com.c3s.ai.common.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * The reference report's number formats, for the JSP. js/report.js has the same functions for the
 * what-if controls, so a figure reads the same before and after it's recalculated - change both.
 */
public final class ReportFormat {

    /** A bar segment narrower than this carries no label; js/report.js reads it off the page. */
    public static final int MIN_LABELLED_SEGMENT_PCT = 13;

    private ReportFormat() {
    }

    /** "15.0%" on a segment wide enough to hold it, otherwise nothing. */
    public static String segmentLabel(double pct) {
        return pct >= MIN_LABELLED_SEGMENT_PCT ? pct1(pct) : "";
    }

    /** The label with its emoji in front, or "" for a profile with no category. */
    public static String category(String emoji, String label) {
        if (label == null) {
            return "";
        }
        return emoji != null ? emoji + " " + label : label;
    }

    /** "$1,235" - whole dollars, half up. */
    public static String money0(BigDecimal value) {
        return "$" + Money.num(value.setScale(0, RoundingMode.HALF_UP));
    }

    public static String money0(long value) {
        return "$" + Money.num(value);
    }

    /** "$1,234.56". */
    public static String money(BigDecimal value) {
        return Money.money(value);
    }

    /** "1,234". */
    public static String num(long value) {
        return Money.num(value);
    }

    /** part as a percentage of whole; 0 when whole is 0. */
    public static double pctOf(BigDecimal part, BigDecimal whole) {
        return whole.signum() > 0 ? part.doubleValue() / whole.doubleValue() * 100 : 0;
    }

    /** "15.0¢" - cents of each dollar collected. */
    public static String perDollar(BigDecimal part, BigDecimal whole) {
        return fixed1(pctOf(part, whole)) + "¢";
    }

    /** "15.0%". */
    public static String pct1(double pct) {
        return fixed1(pct) + "%";
    }

    /** "15.00" - a bar's data-bar-pct, which progress-bars.js turns into its width. */
    public static String barPct(double pct) {
        return String.format(Locale.US, "%.2f", pct);
    }

    /** The fee rate as the reference prints it: 2.90 -> "2.9", 3.00 -> "3". */
    public static String rate(BigDecimal pct) {
        BigDecimal stripped = pct.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /** "$12.4K" - a weekly bar's label. */
    public static String thousands(long value) {
        return "$" + BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000)).setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + "K";
    }

    /** "−$1,234" (a minus sign, not a hyphen), or "$0" when nothing was taken. */
    public static String deduction(BigDecimal value) {
        return value.signum() > 0 ? "−" + money0(value) : "$0";
    }

    static String fixed1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
