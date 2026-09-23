package com.c3s.ai.admin.allocation;

import com.c3s.ai.common.util.DbEnums;

/** Who absorbs the payment processor's fee on a donation. */
public enum FeeModel {
    DEDUCTED,
    ADDED,
    ABSORBED;

    public String dbValue() {
        return DbEnums.toDb(this);
    }

    /** From a DB row - a bad value means the row is corrupt, so throw. */
    public static FeeModel fromDbValue(String value) {
        return DbEnums.fromDb(FeeModel.class, value, DEDUCTED);
    }

    /** From the settings form - a bad value is just user input, returns null for a field error. */
    public static FeeModel fromRequestValue(String value) {
        return DbEnums.fromRequest(FeeModel.class, value);
    }
}
