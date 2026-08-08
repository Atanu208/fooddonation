package com.app.fooddonation.exception;

/**
 * Thrown when the authenticated principal attempts an action they are
 * not allowed to perform on a resource. Maps to HTTP 403 Forbidden.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
