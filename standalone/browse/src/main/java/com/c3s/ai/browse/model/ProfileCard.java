package com.c3s.ai.browse.model;

import java.util.List;

public record ProfileCard(
        String name,
        String blurb,
        String emoji,
        int goal,
        int raised,
        int percent,
        String tagLabel,
        String tagClass,
        String cardBackground,
        List<Long> categoryIds,
        long weeklyRaised) {

    public boolean isWeekly() {
        return weeklyRaised > 0;
    }

    // read by browse.js for the category filter
    public String categoryIdsAttr() {
        StringBuilder sb = new StringBuilder();
        for (Long cid : categoryIds) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(cid);
        }
        return sb.toString();
    }
}
