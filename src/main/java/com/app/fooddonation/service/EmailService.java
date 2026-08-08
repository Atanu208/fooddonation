package com.app.fooddonation.service;

import com.app.fooddonation.model.Notification;
import com.app.fooddonation.model.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email notifications. Uses whatever SMTP server is
 * configured - Mailpit (self-hosted, free) in dev, any provider in prod.
 * Disabled via {@code app.mail.enabled=false} in tests so the suite never
 * touches a mail server.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.baseUrl = baseUrl;
    }

    @Async("asyncExecutor")
    public void sendNotificationEmail(User user, Notification notification) {
        if (!enabled) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("[FoodShare] " + notification.getTitle());
            helper.setText(buildHtml(notification), true);
            mailSender.send(message);
            log.debug("Email sent to {}: {}", user.getEmail(), notification.getTitle());
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    private String buildHtml(Notification notification) {
        String detailUrl = baseUrl + "/notifications";
        String donationUrl = notification.getDonationId() != null
                ? baseUrl + "/donation/" + notification.getDonationId()
                : detailUrl;

        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
                  <div style="background:#16a34a;padding:16px 24px">
                    <span style="color:#fff;font-size:20px;font-weight:bold">&#127860; FoodShare</span>
                  </div>
                  <div style="padding:24px">
                    <h2 style="margin-top:0;color:#111827">%s</h2>
                    <p style="color:#374151;line-height:1.6">%s</p>
                    <p style="margin-top:24px">
                      <a href="%s" style="background:#16a34a;color:#fff;padding:10px 18px;border-radius:6px;text-decoration:none">View details</a>
                    </p>
                  </div>
                  <div style="padding:12px 24px;background:#f9fafb;color:#6b7280;font-size:12px">
                    You are receiving this because you use FoodShare. Manage alerts from your notifications page.
                  </div>
                </div>
                """.formatted(esc(notification.getTitle()), esc(notification.getMessage()), donationUrl);
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
