package com.c3s.ai.admin.profile;

import com.c3s.ai.common.util.Strings;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Server-side field rules for a profile - the form's own checks can be bypassed. */
final class ProfileValidator {

    private static final int GOAL_MIN = 100;
    private static final Pattern STATE_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    private ProfileValidator() {
    }

    static Map<String, String> validate(Profile profile, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (Strings.isBlank(profile.getName())) {
            errors.put("name", "Name or alias is required.");
        } else if (Strings.longerThan(profile.getName(), Profile.NAME_MAX)) {
            errors.put("name", "Name must be " + Profile.NAME_MAX + " characters or fewer.");
        }

        if (Strings.longerThan(profile.getCity(), Profile.CITY_MAX)) {
            errors.put("city", "City must be " + Profile.CITY_MAX + " characters or fewer.");
        }

        if (Strings.isBlank(profile.getBlurb())) {
            errors.put("blurb", "Short blurb is required.");
        } else if (Strings.longerThan(profile.getBlurb(), Profile.BLURB_MAX)) {
            errors.put("blurb", "Blurb must be " + Profile.BLURB_MAX + " characters or fewer.");
        }

        if (Strings.longerThan(profile.getStory(), Profile.STORY_MAX)) {
            errors.put("story", "Story must be " + Profile.STORY_MAX + " characters or fewer.");
        }

        if (Strings.longerThan(profile.getOwnEmoji(), Profile.EMOJI_MAX)) {
            errors.put("emoji", "Emoji must be " + Profile.EMOJI_MAX + " characters or fewer.");
        }

        if (profile.getCategories().isEmpty()) {
            errors.put("categories", "Please choose at least one category.");
        }

        if (profile.getGoal() == null) {
            errors.put("goal", "Goal amount is required.");
        } else if (profile.getGoal() < GOAL_MIN) {
            errors.put("goal", "Goal must be at least $" + GOAL_MIN + ".");
        }

        if (profile.getRaised() == null) {
            errors.put("raised", "Already-raised amount is required (use 0 if none).");
        } else if (profile.getRaised() < 0) {
            errors.put("raised", "Already-raised amount cannot be negative.");
        } else if (profile.getGoal() != null && profile.getRaised() > profile.getGoal()) {
            errors.put("raised", "Already-raised amount cannot exceed the goal.");
        }

        if (profile.getDeadline() != null && profile.getDeadline().isBefore(today)) {
            errors.put("deadline", "Deadline cannot be in the past.");
        }

        String state = profile.getState();
        if (!Strings.isBlank(state) && !STATE_PATTERN.matcher(state).matches()) {
            errors.put("state", "State must be a 2-letter code (e.g. FL).");
        }

        return errors;
    }
}
