package com.c3s.ai.admin.util;

import java.util.Locale;

/** Flash-toast wording in one place instead of scattered through the JSPs. */
public final class FlashMessages {

    private static final String CREATED = "%s \"%s\" created successfully.";
    private static final String UPDATED = "%s \"%s\" updated successfully.";
    private static final String ARCHIVED = "%s \"%s\" archived successfully.";
    private static final String BLOCKED_EDIT = "Archived %s can't be edited.";
    private static final String DRAFT_SAVED = "\"%s\" saved as a draft.";
    private static final String PUBLISHED = "\"%s\" published to donor site!";
    private static final String POLICY_SAVED_DEFAULT = "Default allocation policy saved.";
    private static final String POLICY_SAVED_CATEGORY = "Allocation policy saved for \"%s\".";
    private static final String VALIDATION_FAILED = "Please fix the highlighted errors before saving.";

    private FlashMessages() {
    }

    public static String created(String entity, String name) {
        return bind(CREATED, entity, name);
    }

    public static String updated(String entity, String name) {
        return bind(UPDATED, entity, name);
    }

    public static String archived(String entity, String name) {
        return bind(ARCHIVED, entity, name);
    }

    public static String blockedEdit(String entityPlural) {
        return bind(BLOCKED_EDIT, entityPlural);
    }

    public static String draftSaved(String name) {
        return bind(DRAFT_SAVED, name);
    }

    public static String published(String name) {
        return bind(PUBLISHED, name);
    }

    /** categoryLabel null = the default policy was saved. */
    public static String policySaved(String categoryLabel) {
        return categoryLabel != null ? bind(POLICY_SAVED_CATEGORY, categoryLabel) : POLICY_SAVED_DEFAULT;
    }

    public static String validationFailed() {
        return VALIDATION_FAILED;
    }

    private static String bind(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }
}
