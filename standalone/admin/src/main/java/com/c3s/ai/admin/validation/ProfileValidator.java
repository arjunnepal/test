package com.c3s.ai.admin.validation;

import com.c3s.ai.admin.model.Profile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Server-side profile validation - the form's own checks can be bypassed. */
public final class ProfileValidator {

    private static final int NAME_MAX = 100;
    private static final int BLURB_MAX = 160;
    private static final int STORY_MAX = 2000;
    private static final int GOAL_MIN = 100;
    private static final Pattern STATE_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    private ProfileValidator() {
    }

    public static Map<String, String> validate(Profile profile) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(profile.getName())) {
            errors.put("name", "Name or alias is required.");
        } else if (profile.getName().length() > NAME_MAX) {
            errors.put("name", "Name must be " + NAME_MAX + " characters or fewer.");
        }

        if (isBlank(profile.getBlurb())) {
            errors.put("blurb", "Short blurb is required.");
        } else if (profile.getBlurb().length() > BLURB_MAX) {
            errors.put("blurb", "Blurb must be " + BLURB_MAX + " characters or fewer.");
        }

        if (profile.getStory() != null && profile.getStory().length() > STORY_MAX) {
            errors.put("story", "Story must be " + STORY_MAX + " characters or fewer.");
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

        if (profile.getDeadline() != null && profile.getDeadline().isBefore(LocalDate.now())) {
            errors.put("deadline", "Deadline cannot be in the past.");
        }

        String state = profile.getState();
        if (state != null && !state.isBlank() && !STATE_PATTERN.matcher(state).matches()) {
            errors.put("state", "State must be a 2-letter code (e.g. FL).");
        }

        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
