package com.c3s.ai.common.util;

/** Strip line breaks and control chars out of request-derived values before logging them (log forging). */
public final class Log {

    private static final int MAX_LEN = 256;

    private Log() {
    }

    /**
     * Single-line, truncated to 256 chars, null -> "null". Control characters and U+0085/2028/2029
     * become '_', because some log viewers treat those as new lines too.
     */
    public static String clean(Object value) {
        String s = String.valueOf(value);
        if (s.length() > MAX_LEN) {
            s = s.substring(0, MAX_LEN) + "...";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(isUnsafe(c) ? '_' : c);
        }
        return out.toString();
    }

    private static boolean isUnsafe(char c) {
        return c < 0x20 || (c >= 0x7f && c <= 0x9f) || c == ' ' || c == ' ';
    }
}
