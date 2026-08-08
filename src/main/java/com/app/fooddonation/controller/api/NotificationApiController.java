package com.app.fooddonation.controller.api;

import com.app.fooddonation.dto.MessageResponse;
import com.app.fooddonation.dto.NotificationDto;
import com.app.fooddonation.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Read the current user's in-app notifications")
public class NotificationApiController {

    private final NotificationQueryService notificationQueryService;

    public NotificationApiController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    @Operation(summary = "List my notifications (newest first)")
    public ResponseEntity<List<NotificationDto>> myNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationQueryService.getNotifications(authentication.getName())
                .stream()
                .map(NotificationDto::from)
                .toList());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count",
                notificationQueryService.getUnreadCount(authentication.getName())));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<MessageResponse> markRead(@PathVariable Long id, Authentication authentication) {
        notificationQueryService.markAsRead(id, authentication.getName());
        return ResponseEntity.ok(MessageResponse.of("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<MessageResponse> markAllRead(Authentication authentication) {
        notificationQueryService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(MessageResponse.of("All notifications marked as read"));
    }
}
