package com.c3s.ai.admin.util;

import java.util.Locale;

// JSP helpers - we render with scriptlets, not JSTL/EL, so esc() stands in for fn:escapeXml
// and money/num/pct1 for fmt:formatNumber. All null-safe, null -> "" (EL prints "null" otherwise).
public final class HtmlUtil {

    private HtmlUtil() {
    }

    /** Escapes & < > " ' - safe for element text and quoted attribute values. */
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

    /** One decimal place, e.g. "13.3" - allocation preview percent labels. */
    public static String pct1(Number value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%.1f", value.doubleValue());
    }
}
