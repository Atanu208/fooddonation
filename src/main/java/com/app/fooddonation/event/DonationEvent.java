package com.app.fooddonation.event;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;

import java.time.LocalDateTime;

/**
 * Domain event published whenever a donation changes state. The listener runs
 * asynchronously AFTER the publishing transaction commits, so a failure to
 * notify never rolls back the core business operation.
 */
public record DonationEvent(
        Long donationId,
        String foodDescription,
        String donorEmail,
        String ngoEmail,
        DonationStatus newStatus,
        String actorEmail,
        String reason,
        LocalDateTime occurredAt) {

    public static DonationEvent of(Donation donation, DonationStatus newStatus, String actorEmail) {
        return of(donation, newStatus, actorEmail, null);
    }

    public static DonationEvent of(Donation donation, DonationStatus newStatus,
                                   String actorEmail, String reason) {
        return new DonationEvent(
                donation.getId(),
                donation.getFoodDescription(),
                donation.getDonor() != null ? donation.getDonor().getEmail() : null,
                donation.getNgo() != null ? donation.getNgo().getEmail() : null,
                newStatus,
                actorEmail,
                reason,
                LocalDateTime.now());
    }
}
