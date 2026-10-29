package com.c3s.ai.report.policy;

import com.c3s.ai.common.util.DbEnums;

/** Who pays the processor's fee - uwnefl_allocation_settings.fee_model, read-only here. */
public enum FeeModel {
    DEDUCTED("Deducted from gift", "Deducted"),
    ADDED("Donor covers fee", "Donor covers"),
    ABSORBED("UWNEFL absorbs fee", "UWNEFL absorbs");

    private final String label;
    private final String buttonLabel;

    FeeModel(String label, String buttonLabel) {
        this.label = label;
        this.buttonLabel = buttonLabel;
    }

    /** The report's wording for the policy note. */
    public String label() {
        return label;
    }

    /** The what-if control's wording. */
    public String buttonLabel() {
        return buttonLabel;
    }

    /** "deducted" - also the report's data-model value, which js/report.js reads. */
    public String dbValue() {
        return DbEnums.toDb(this);
    }

    /** From a DB row - a bad value means the row is corrupt, so throw. */
    public static FeeModel fromDbValue(String value) {
        return DbEnums.fromDb(FeeModel.class, value, DEDUCTED);
    }
}
