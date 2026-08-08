package com.app.fooddonation.service;

import com.app.fooddonation.exception.ResourceNotFoundException;
import com.app.fooddonation.model.Notification;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side of the notification pipeline. The write side is the async event
 * listener in {@link NotificationService}; reads stay lightweight and separate.
 */
@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationQueryService(NotificationRepository notificationRepository,
                                    UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public List<Notification> getNotifications(String email) {
        User user = userService.findByEmail(email);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String email) {
        User user = userService.findByEmail(email);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = userService.findByEmail(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new com.app.fooddonation.exception.UnauthorizedActionException(
                    "You cannot modify another user's notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userService.findByEmail(email);
        notificationRepository.markAllAsRead(user);
    }
}
