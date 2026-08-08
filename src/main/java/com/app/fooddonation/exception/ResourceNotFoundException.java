package com.app.fooddonation.exception;

/**
 * Thrown when a requested resource (donation, user, etc.) does not exist.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException donation(Long id) {
        return new ResourceNotFoundException("Donation not found with id: " + id);
    }

    public static ResourceNotFoundException user(String email) {
        return new ResourceNotFoundException("User not found with email: " + email);
    }
}
