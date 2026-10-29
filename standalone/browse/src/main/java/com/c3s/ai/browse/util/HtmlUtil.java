package com.c3s.ai.browse.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class HtmlUtil {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000);
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private HtmlUtil() {
    }

    public static String esc(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&#34;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String num(Number value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%,d", value.longValue());
    }

    // 842000 -> "842K", 12400 -> "12.4K", 1250000 -> "1.3M"
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
