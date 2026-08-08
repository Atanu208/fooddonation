package com.app.fooddonation.config;

import com.app.fooddonation.service.NotificationQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects globally-available view attributes (e.g. unread notification count)
 * into every server-rendered page for the authenticated user.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private NotificationQueryService notificationQueryService;

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return 0;
        }
        try {
            return notificationQueryService.getUnreadCount(authentication.getName());
        } catch (Exception ex) {
            return 0;
        }
    }

    @ModelAttribute("currentRole")
    public String currentRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "GUEST";
        }
        String authority = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("USER");
        return authority.substring(5);
    }

    @ModelAttribute("currentEmail")
    public String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }
        return authentication.getName();
    }
}
