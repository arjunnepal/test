package com.c3s.ai.common.exception;

/** A value read back from the DB doesn't match what the app expects (e.g. a bad enum string). */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
