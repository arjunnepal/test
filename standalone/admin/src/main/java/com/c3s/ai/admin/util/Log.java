package com.c3s.ai.admin.util;

/** Strip CR/LF/control chars out of request-derived values before logging them (log forging). */
public final class Log {

    private static final int MAX_LEN = 256;

    private Log() {
    }

    /** Single-line, control chars become '_', truncated to 256 chars. null -> "null". */
    public static String clean(Object value) {
        String s = String.valueOf(value);
        if (s.length() > MAX_LEN) {
            s = s.substring(0, MAX_LEN) + "...";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c < 0x20 || c == 0x7f ? '_' : c);
        }
        return out.toString();
    }
}
