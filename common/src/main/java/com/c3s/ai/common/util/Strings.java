package com.c3s.ai.common.util;

public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Trimmed value, or null when blank. */
    public static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    /** True when non-null and longer than max characters (code points, so an emoji counts once). */
    public static boolean longerThan(String s, int max) {
        return s != null && s.codePointCount(0, s.length()) > max;
    }
}
