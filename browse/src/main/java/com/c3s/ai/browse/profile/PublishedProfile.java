package com.c3s.ai.browse.profile;

import com.c3s.ai.common.util.ProfileMath;

import java.util.List;

/**
 * A published profile, holding exactly what a giving-page card shows - no story, no location,
 * no review flags, no drafts. Categories are active ones only, ordered by label.
 */
public record PublishedProfile(long id, String name, String blurb, String emoji, int goal, int raised,
                               List<CardCategory> categories) {

    public PublishedProfile {
        categories = List.copyOf(categories);
    }

    /** The category that styles the card (colour and tag), or null when it has none. */
    public CardCategory firstCategory() {
        return categories.isEmpty() ? null : categories.get(0);
    }

    public String displayEmoji() {
        CardCategory first = firstCategory();
        return ProfileMath.displayEmoji(emoji, first != null ? first.emoji() : null);
    }

    public int percentRaised() {
        return ProfileMath.percentRaised(goal, raised);
    }
}
