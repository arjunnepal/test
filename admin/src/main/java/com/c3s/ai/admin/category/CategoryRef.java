package com.c3s.ai.admin.category;

/** A category as other pages show it - a tag, a picker option, a list row - without its status. */
public record CategoryRef(long id, String label, String emoji, String colorClass) {

    /** A category known only by id, as submitted from a form checkbox; resolved later. */
    public static CategoryRef idOnly(long id) {
        return new CategoryRef(id, null, null, null);
    }
}
