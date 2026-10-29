package com.c3s.ai.admin.exception;

public class EntityNotFoundException extends Exception {

    public EntityNotFoundException(String entityName, long id) {
        super("No " + entityName + " found with id " + id);
    }
}
