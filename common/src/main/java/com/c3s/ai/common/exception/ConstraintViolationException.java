package com.c3s.ai.common.exception;

/**
 * A write the database refused on an integrity constraint (SQLState class 23). Usually a race the
 * validator couldn't see, so callers turn it into a form error rather than a 500.
 */
public class ConstraintViolationException extends DataAccessException {

    public ConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    /** True when the driver's message names this constraint or key (e.g. "uwnefl_categories.label"). */
    public boolean involves(String constraintOrKey) {
        Throwable cause = getCause();
        String detail = cause != null ? cause.getMessage() : null;
        return detail != null && constraintOrKey != null && detail.contains(constraintOrKey);
    }
}
