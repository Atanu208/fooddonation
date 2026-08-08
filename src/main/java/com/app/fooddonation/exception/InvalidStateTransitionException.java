package com.app.fooddonation.exception;

import com.app.fooddonation.model.DonationStatus;

/**
 * Thrown when a donation status transition is not allowed by the
 * state machine. Maps to HTTP 409 Conflict.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public static InvalidStateTransitionException between(DonationStatus from, DonationStatus to) {
        return new InvalidStateTransitionException(
                "Invalid status transition from " + from.getDisplayName()
                        + " to " + to.getDisplayName());
    }
}
