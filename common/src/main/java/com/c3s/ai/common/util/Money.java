package com.c3s.ai.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Number formatting shared by the Java code and the JSPs. All null-safe: null -> "". */
public final class Money {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000);
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private Money() {
    }

    /** "$1,234.56" (en_US). */
    public static String money(Number value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "$%,.2f", value.doubleValue());
    }

    /** "1,234" - grouped, no decimals. */
    public static String num(Number value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%,d", value.longValue());
    }

    /** One decimal place, e.g. "13.3". */
    public static String pct1(Number value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%.1f", value.doubleValue());
    }

    /** Compact money for stat tiles: 842000 -> "842K", 12400 -> "12.4K", 1250000 -> "1.3M". */
    public static String compactMoney(Number value) {
        if (value == null) {
            return "";
        }
        long v = value.longValue();
        if (Math.abs(v) < 1_000) {
            return String.valueOf(v);
        }
        BigDecimal thousands = BigDecimal.valueOf(v).divide(THOUSAND).setScale(1, RoundingMode.HALF_UP);
        // otherwise 999,950 would show as 1000K
        if (thousands.abs().compareTo(THOUSAND) < 0) {
            return plain(thousands) + "K";
        }
        return plain(BigDecimal.valueOf(v).divide(MILLION).setScale(1, RoundingMode.HALF_UP)) + "M";
    }

    private static String plain(BigDecimal d) {
        return d.stripTrailingZeros().toPlainString();
    }
}
