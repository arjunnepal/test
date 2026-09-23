package com.c3s.ai.core.web;

import com.c3s.ai.common.util.Money;
import com.c3s.ai.core.auth.Csrf;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JSP helpers. The views render with scriptlets, not JSTL/EL, so esc() stands in for fn:escapeXml
 * and money/num/pct1 for fmt:formatNumber. All null-safe, with null rendering as "".
 */
public final class HtmlUtil {

    private HtmlUtil() {
    }

    /** Escapes & < > " ' - safe for element text and quoted attribute values. Not for JS, CSS or URLs. */
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

    /** The hidden CSRF field for a POST form. Mints the token, so call it before the response commits. */
    public static String csrfInput(HttpServletRequest request) {
        return "<input type=\"hidden\" name=\"" + Csrf.PARAM + "\" value=\"" + esc(Csrf.tokenFor(request)) + "\"/>";
    }

    /** The error span under a form field, or "" when that field is fine. */
    public static String fieldError(FormPageData data, String field) {
        String error = data == null ? null : data.getError(field);
        return error == null ? "" : "<span class=\"field-error\">" + esc(error) + "</span>";
    }

    public static String money(Number value) {
        return Money.money(value);
    }

    public static String num(Number value) {
        return Money.num(value);
    }

    public static String pct1(Number value) {
        return Money.pct1(value);
    }

    public static String compactMoney(Number value) {
        return Money.compactMoney(value);
    }
}
