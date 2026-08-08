package com.app.fooddonation.exception;

/**
 * Thrown when an API client exceeds an allowed rate limit.
 * Maps to HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
