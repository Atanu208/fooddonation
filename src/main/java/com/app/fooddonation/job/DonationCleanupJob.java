package com.app.fooddonation.job;

import com.app.fooddonation.service.DonationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically expires PENDING donations whose pickup window has passed.
 * Keeps the marketplace fresh and prevents orphaned listings.
 */
@Component
public class DonationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DonationCleanupJob.class);

    @Autowired
    private DonationService donationService;

    @Scheduled(fixedDelayString = "${app.jobs.expiry-interval-ms:300000}", initialDelayString = "${app.jobs.expiry-initial-delay-ms:15000}")
    public void expireStaleDonations() {
        int expired = donationService.expireStaleDonations();
        if (expired > 0) {
            log.info("Scheduled cleanup expired {} donation(s)", expired);
        }
    }
}
