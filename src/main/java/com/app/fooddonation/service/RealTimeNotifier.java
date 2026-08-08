package com.app.fooddonation.service;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.Notification;
import com.app.fooddonation.model.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Pushes live messages to connected WebSocket clients. Two channels exist:
 * {@code /topic/notifications} carries personal alerts (clients filter by
 * recipient) and {@code /topic/new-donations} notifies NGOs of fresh listings.
 */
@Service
public class RealTimeNotifier {

    public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";
    public static final String TOPIC_NEW_DONATIONS = "/topic/new-donations";

    private final SimpMessagingTemplate messagingTemplate;

    public RealTimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastNewDonation(Donation donation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_DONATION");
        payload.put("donationId", donation.getId());
        payload.put("foodDescription", donation.getFoodDescription());
        payload.put("city", donation.getPickupCity());
        payload.put("quantity", donation.getQuantity());
        payload.put("pickupTime", donation.getPickupTime() != null ? donation.getPickupTime().toString() : null);
        payload.put("donorName", donation.getDonor() != null ? donation.getDonor().getName() : null);
        messagingTemplate.convertAndSend(TOPIC_NEW_DONATIONS, payload);
    }

    public void notifyUser(User user, Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NOTIFICATION");
        payload.put("recipientId", user.getId());
        payload.put("recipientEmail", user.getEmail());
        payload.put("notificationId", notification.getId());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("donationId", notification.getDonationId());
        payload.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
        messagingTemplate.convertAndSend(TOPIC_NOTIFICATIONS, payload);
    }
}
