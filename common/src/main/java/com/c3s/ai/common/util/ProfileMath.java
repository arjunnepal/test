package com.c3s.ai.common.util;

/** The two derived values every view of a profile shows, so each read model computes them alike. */
public final class ProfileMath {

    private ProfileMath() {
    }

    /** Percent of goal raised, clamped to 0..100; 0 when there's no usable goal. */
    public static int percentRaised(Integer goal, Integer raised) {
        if (goal == null || goal <= 0 || raised == null) {
            return 0;
        }
        return Math.min(100, Math.round(100f * raised / goal));
    }

    /** The profile's own emoji, else its first category's, else null. */
    public static String displayEmoji(String ownEmoji, String firstCategoryEmoji) {
        if (!Strings.isBlank(ownEmoji)) {
            return ownEmoji;
        }
        return firstCategoryEmoji;
    }
}
