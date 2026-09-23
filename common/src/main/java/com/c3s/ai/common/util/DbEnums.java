package com.c3s.ai.common.util;

import com.c3s.ai.common.exception.InvalidDataException;

import java.util.Locale;

/** Enum <-> lower-case DB value, shared by every status/type column. */
public final class DbEnums {

    private DbEnums() {
    }

    public static String toDb(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    /** From the database: null gives whenNull, anything unrecognised means a corrupt row and throws. */
    public static <E extends Enum<E>> E fromDb(Class<E> type, String value, E whenNull) {
        if (value == null) {
            return whenNull;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Unrecognized " + type.getSimpleName() + " in database: " + value, e);
        }
    }

    /** Parse user input: blank or unrecognised -> null, for the caller to report as a field error. */
    public static <E extends Enum<E>> E fromRequest(Class<E> type, String value) {
        if (Strings.isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
