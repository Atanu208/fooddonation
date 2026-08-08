package com.app.fooddonation.controller;

import com.app.fooddonation.model.Notification;
import com.app.fooddonation.service.NotificationQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class NotificationController {

    @Autowired
    private NotificationQueryService notificationQueryService;

    @GetMapping("/notifications")
    public String notifications(Authentication authentication, Model model) {
        List<Notification> notifications = notificationQueryService.getNotifications(authentication.getName());
        model.addAttribute("notifications", notifications);
        model.addAttribute("unread", notificationQueryService.getUnreadCount(authentication.getName()));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            notificationQueryService.markAsRead(id, authentication.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(Authentication authentication) {
        notificationQueryService.markAllAsRead(authentication.getName());
        return "redirect:/notifications";
    }
}
