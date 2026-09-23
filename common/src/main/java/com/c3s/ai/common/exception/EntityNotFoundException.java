package com.c3s.ai.common.exception;

public class EntityNotFoundException extends Exception {

    public EntityNotFoundException(String entityName, long id) {
        super("No " + entityName + " found with id " + id);
    }
}
