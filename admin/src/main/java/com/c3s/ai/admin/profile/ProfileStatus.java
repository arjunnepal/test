package com.c3s.ai.admin.profile;

import com.c3s.ai.common.util.DbEnums;

/** Publication state of a recipient profile. */
public enum ProfileStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    public String dbValue() {
        return DbEnums.toDb(this);
    }

    public static ProfileStatus fromDbValue(String value) {
        return DbEnums.fromDb(ProfileStatus.class, value, DRAFT);
    }

    public String label() {
        return switch (this) {
            case DRAFT -> "Draft";
            case PUBLISHED -> "Published";
            case ARCHIVED -> "Archived";
        };
    }

    /** CSS class for the status tag in the dashboard lists. */
    public String badgeClass() {
        return switch (this) {
            case DRAFT -> "status-draft";
            case PUBLISHED -> "status-published";
            case ARCHIVED -> "pt-tag-disabled";
        };
    }
}
