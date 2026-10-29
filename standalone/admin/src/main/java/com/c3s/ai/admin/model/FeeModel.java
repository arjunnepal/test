package com.c3s.ai.admin.model;

import com.c3s.ai.admin.exception.InvalidDataException;

import java.util.Locale;

/** Who absorbs the payment processor's fee on a donation. */
public enum FeeModel {
    DEDUCTED,
    ADDED,
    ABSORBED;

    public String dbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** From a DB row - a bad value means the row is corrupt, so throw. */
    public static FeeModel fromDbValue(String value) {
        if (value == null) {
            return DEDUCTED;
        }
        try {
            return FeeModel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Unrecognized fee model in database: " + value, e);
        }
    }

    /** From the settings form - a bad value is just user input, returns null for a field error. */
    public static FeeModel fromRequestValue(String value) {
        return parse(value);
    }

    private static FeeModel parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return FeeModel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
