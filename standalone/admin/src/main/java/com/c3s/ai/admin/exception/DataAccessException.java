package com.c3s.ai.admin.exception;

/** Unchecked wrapper for SQLException so nothing above the DAO layer has to import java.sql. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
