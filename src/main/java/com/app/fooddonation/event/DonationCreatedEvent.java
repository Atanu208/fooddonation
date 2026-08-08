package com.app.fooddonation.event;

import com.app.fooddonation.model.Donation;

/**
 * Published when a new donation is listed so NGOs can be alerted in real time.
 * The listener runs asynchronously AFTER the publishing transaction commits.
 */
public record DonationCreatedEvent(Donation donation) {
}
