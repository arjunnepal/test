package com.c3s.ai.admin.category;

import com.c3s.ai.common.util.DbEnums;

/** Whether a category is available for new profiles to pick from. */
public enum CategoryStatus {
    ACTIVE,
    ARCHIVED;

    public String dbValue() {
        return DbEnums.toDb(this);
    }

    public static CategoryStatus fromDbValue(String value) {
        return DbEnums.fromDb(CategoryStatus.class, value, ACTIVE);
    }

    public String label() {
        return this == ACTIVE ? "Active" : "Archived";
    }
}
