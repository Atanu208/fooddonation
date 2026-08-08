package com.app.fooddonation.service;

import com.app.fooddonation.event.DonationEvent;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Notification;
import com.app.fooddonation.model.NotificationType;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.NotificationRepository;
import com.app.fooddonation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Consumes {@link DonationEvent}s after commit and persists in-app
 * notifications for the affected donor and NGO. Fully asynchronous so the
 * notification pipeline is decoupled from the request lifecycle.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RealTimeNotifier realTimeNotifier;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               RealTimeNotifier realTimeNotifier,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.realTimeNotifier = realTimeNotifier;
        this.emailService = emailService;
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDonationEvent(DonationEvent event) {
        try {
            switch (event.newStatus()) {
                case ACCEPTED -> {
                    notifyUser(event.donorEmail(), NotificationType.DONATION_ACCEPTED,
                            "Your donation was accepted",
                            "An NGO accepted your donation \"" + shortDesc(event.foodDescription()) + "\".",
                            event.donationId());
                }
                case COMPLETED -> {
                    notifyUser(event.donorEmail(), NotificationType.DONATION_STATUS_CHANGED,
                            "Donation completed",
                            "Your donation \"" + shortDesc(event.foodDescription()) + "\" was marked completed.",
                            event.donationId());
                    notifyUser(event.ngoEmail(), NotificationType.DONATION_STATUS_CHANGED,
                            "Donation completed",
                            "You completed the donation \"" + shortDesc(event.foodDescription()) + "\".",
                            event.donationId());
                }
                case CANCELLED -> {
                    String reason = event.reason() != null ? event.reason() : "not specified";
                    notifyUser(event.donorEmail(), NotificationType.DONATION_CANCELLED,
                            "Donation cancelled",
                            "Your donation \"" + shortDesc(event.foodDescription()) + "\" was cancelled. Reason: " + reason,
                            event.donationId());
                    if (event.ngoEmail() != null) {
                        notifyUser(event.ngoEmail(), NotificationType.DONATION_CANCELLED,
                                "Donation cancelled",
                                "The donation \"" + shortDesc(event.foodDescription()) + "\" you claimed was cancelled.",
                                event.donationId());
                    }
                }
                case EXPIRED -> notifyUser(event.donorEmail(), NotificationType.DONATION_EXPIRED,
                        "Donation expired",
                        "Your donation \"" + shortDesc(event.foodDescription()) + "\" expired because no NGO claimed it before pickup time.",
                        event.donationId());
                case PICKED_UP -> {
                    notifyUser(event.donorEmail(), NotificationType.DONATION_STATUS_CHANGED,
                            "Donation picked up",
                            "Your donation \"" + shortDesc(event.foodDescription()) + "\" was picked up.",
                            event.donationId());
                }
                case DELIVERED -> {
                    notifyUser(event.ngoEmail(), NotificationType.DONATION_STATUS_CHANGED,
                            "Donation delivered",
                            "Donation \"" + shortDesc(event.foodDescription()) + "\" delivered.",
                            event.donationId());
                }
                default -> { }
            }
        } catch (Exception ex) {
            log.warn("Failed to process donation event for id={}: {}", event.donationId(), ex.getMessage());
        }
    }

    private void notifyUser(String email, NotificationType type, String title,
                            String message, Long donationId) {
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email).ifPresent(user -> {
            Notification notification = notificationRepository.save(new Notification(user, type, title, message, donationId));
            log.debug("Notification created for {}: {}", email, title);
            realTimeNotifier.notifyUser(user, notification);
            emailService.sendNotificationEmail(user, notification);
        });
    }

    private String shortDesc(String description) {
        if (description == null) {
            return "food donation";
        }
        return description.length() > 40 ? description.substring(0, 40) + "..." : description;
    }
}
