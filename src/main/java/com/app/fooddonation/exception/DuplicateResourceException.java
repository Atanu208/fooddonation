package com.app.fooddonation.exception;

/**
 * Thrown when a unique constraint is violated (e.g. duplicate email).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
