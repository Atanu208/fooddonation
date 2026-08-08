package com.app.fooddonation.service;

import com.app.fooddonation.event.DonationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to newly listed donations and broadcasts them to connected NGOs over
 * WebSocket. Decoupled from the request lifecycle (async, after commit).
 */
@Component
public class RealTimeDonationListener {

    private static final Logger log = LoggerFactory.getLogger(RealTimeDonationListener.class);

    private final RealTimeNotifier realTimeNotifier;

    public RealTimeDonationListener(RealTimeNotifier realTimeNotifier) {
        this.realTimeNotifier = realTimeNotifier;
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDonationCreated(DonationCreatedEvent event) {
        try {
            realTimeNotifier.broadcastNewDonation(event.donation());
        } catch (Exception ex) {
            log.warn("Failed to broadcast new donation id={}: {}",
                    event.donation().getId(), ex.getMessage());
        }
    }
}
