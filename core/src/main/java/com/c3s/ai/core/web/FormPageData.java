package com.c3s.ai.core.web;

import java.util.Map;

/** Base for the add/edit form page objects - adds the per-field error map and the edit flag. */
public abstract class FormPageData extends PageData {

    private Map<String, String> errors;
    private boolean edit;

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    /** null if no error for that field (and null-safe if errors were never set). */
    public String getError(String field) {
        return errors == null ? null : errors.get(field);
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /** true when editing an existing row, false when creating one. */
    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }
}
