package com.c3s.ai.admin.model;

import com.c3s.ai.admin.exception.InvalidDataException;

import java.util.Locale;

/** Whether a category is available for new profiles to pick from. */
public enum CategoryStatus {
    ACTIVE,
    ARCHIVED;

    public String dbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CategoryStatus fromDbValue(String value) {
        if (value == null) {
            return ACTIVE;
        }
        try {
            return CategoryStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Unrecognized category status in database: " + value, e);
        }
    }
}
