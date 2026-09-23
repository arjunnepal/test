package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.util.ProfileMath;
import com.c3s.ai.common.util.Strings;

import java.util.List;

/** A row of the dashboard's profile list - no story, blurb, deadline or review flags. */
public record ProfileListItem(long id, String name, String city, String state, String emoji, String photoFilename,
                              int goal, int raised, ProfileStatus status, List<CategoryRef> categories) {

    public ProfileListItem {
        categories = List.copyOf(categories);
    }

    public boolean hasPhoto() {
        return !Strings.isBlank(photoFilename);
    }

    public String displayEmoji() {
        return ProfileMath.displayEmoji(emoji, categories.isEmpty() ? null : categories.get(0).emoji());
    }

    public int percentRaised() {
        return ProfileMath.percentRaised(goal, raised);
    }

    public String location() {
        return Location.of(city, state);
    }

    public boolean isArchived() {
        return status == ProfileStatus.ARCHIVED;
    }
}
