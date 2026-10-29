package com.c3s.ai.admin.web;

import java.util.Map;

/** Base for the add/edit form page objects - adds the per-field error map. */
public abstract class FormPageData extends PageData {

    private Map<String, String> errors;

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    /** null if no error for that field (and null-safe if errors were never set). */
    public String getError(String field) {
        return errors == null ? null : errors.get(field);
    }
}
