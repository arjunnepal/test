package com.c3s.ai.browse.util;

public final class Log {

    private static final int MAX_LEN = 256;

    private Log() {
    }

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
