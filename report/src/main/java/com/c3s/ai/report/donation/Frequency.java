package com.c3s.ai.report.donation;

import com.c3s.ai.common.util.DbEnums;

/** Whether a donation is a one-off or repeats every month. */
public enum Frequency {
    ONE_TIME("One-time"),
    MONTHLY("Monthly");

    private final String label;

    Frequency(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** From a DB row - a bad value means the row is corrupt, so throw. */
    public static Frequency fromDbValue(String value) {
        return DbEnums.fromDb(Frequency.class, value, ONE_TIME);
    }
}
