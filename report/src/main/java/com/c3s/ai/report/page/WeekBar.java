package com.c3s.ai.report.page;

/** heightPx is 0-100: the tallest bar gets 100px, as in the reference chart. */
public record WeekBar(String valueLabel, int heightPx, String labelTop, String labelBottom, boolean current) {
}
