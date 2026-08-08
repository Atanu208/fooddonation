package com.app.fooddonation.model;

public enum DonationStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted by NGO"),
    PICKED_UP("Picked Up"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

    private final String displayName;

    DonationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}