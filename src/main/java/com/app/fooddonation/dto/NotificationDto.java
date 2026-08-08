package com.app.fooddonation.dto;

import com.app.fooddonation.model.Notification;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        Long donationId,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType() != null ? n.getType().name() : null,
                n.getTitle(),
                n.getMessage(),
                n.getDonationId(),
                n.isRead(),
                n.getCreatedAt());
    }
}
