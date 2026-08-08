package com.app.fooddonation.dto;

import com.app.fooddonation.model.Donation;

import java.time.LocalDateTime;

public record DonationResponse(
        Long id,
        String foodDescription,
        String quantity,
        String foodType,
        boolean packaged,
        String pickupAddress,
        String pickupCity,
        String pickupState,
        String pickupPincode,
        LocalDateTime pickupTime,
        String expiryTime,
        String specialInstructions,
        String status,
        String statusDisplayName,
        Long donorId,
        String donorName,
        Long ngoId,
        String ngoName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(
                d.getId(),
                d.getFoodDescription(),
                d.getQuantity(),
                d.getFoodType(),
                d.isPackaged(),
                d.getPickupAddress(),
                d.getPickupCity(),
                d.getPickupState(),
                d.getPickupPincode(),
                d.getPickupTime(),
                d.getExpiryTime(),
                d.getSpecialInstructions(),
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getStatus() != null ? d.getStatus().getDisplayName() : null,
                d.getDonor() != null ? d.getDonor().getId() : null,
                d.getDonor() != null ? d.getDonor().getName() : null,
                d.getNgo() != null ? d.getNgo().getId() : null,
                d.getNgo() != null ? d.getNgo().getName() : null,
                d.getCreatedAt(),
                d.getUpdatedAt(),
                d.getCompletedAt());
    }
}
