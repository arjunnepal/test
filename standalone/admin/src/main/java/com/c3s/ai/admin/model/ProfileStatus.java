package com.c3s.ai.admin.model;

import com.c3s.ai.admin.exception.InvalidDataException;

import java.util.Locale;

/** Publication state of a recipient profile. */
public enum ProfileStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    public String dbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProfileStatus fromDbValue(String value) {
        if (value == null) {
            return DRAFT;
        }
        try {
            return ProfileStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Unrecognized profile status in database: " + value, e);
        }
    }
}
