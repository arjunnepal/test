package com.c3s.ai.common.exception;

/** The row exists but is archived, and archived rows can't be edited. */
public class ArchivedEntityException extends Exception {

    public ArchivedEntityException(String entityName, long id) {
        super("The " + entityName + " with id " + id + " is archived and can't be edited");
    }
}
