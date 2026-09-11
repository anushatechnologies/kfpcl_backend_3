package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.dto.NotificationRequest;
import com.project.kfpcl_exports.admin.model.DeviceToken;
import com.project.kfpcl_exports.admin.repository.DeviceTokenRepository;
import com.project.kfpcl_exports.buyer.enums.NotificationType;
import com.project.kfpcl_exports.buyer.model.Notification;
import com.project.kfpcl_exports.buyer.repository.NotificationRepository;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import com.project.kfpcl_exports.service.FcmTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController("adminNotificationController")
@RequestMapping("/api/admin")
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository buyerUserRepository;
    private final FcmTokenService fcmTokenService;

    public NotificationController(
            DeviceTokenRepository deviceTokenRepository,
            NotificationRepository notificationRepository,
            @Qualifier("buyerUserRepository") UserRepository buyerUserRepository,
            FcmTokenService fcmTokenService
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
        this.buyerUserRepository = buyerUserRepository;
        this.fcmTokenService = fcmTokenService;
    }

    @PostMapping("/notifications/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody NotificationRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Notification from Admin";
        String message = request.getMessage() != null ? request.getMessage() : "";

        dispatchNotificationToCustomers(title, message);

        return ResponseEntity.ok(Map.of(
                "message", "Notification sent successfully",
                "title", title,
                "recipientType", request.getRecipientType() != null ? request.getRecipientType() : "ALL",
                "success", true
        ));
    }

    @PostMapping("/notifications/send-to-customers")
    public ResponseEntity<Map<String, Object>> sendToCustomers(@RequestBody NotificationRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Notification from Admin";
        String message = request.getMessage() != null ? request.getMessage() : "";

        dispatchNotificationToCustomers(title, message);

        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to all active customers",
                "title", title,
                "success", true
        ));
    }

    @PostMapping("/notifications/send-to-delivery")
    public ResponseEntity<Map<String, Object>> sendToDelivery(@RequestBody NotificationRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Delivery Update";
        String message = request.getMessage() != null ? request.getMessage() : "";

        List<DeviceToken> deliveryTokens = deviceTokenRepository.findByUserType("DELIVERY");
        for (DeviceToken dt : deliveryTokens) {
            try {
                fcmTokenService.sendPushNotification(dt.getToken(), title, message);
            } catch (Exception e) {
                log.warn("Failed to push FCM to delivery token {}: {}", dt.getToken(), e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to all delivery personnel",
                "title", title,
                "success", true
        ));
    }

    private void dispatchNotificationToCustomers(String title, String message) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Save in-app notification for all buyers
        try {
            List<com.project.kfpcl_exports.buyer.model.User> buyers = buyerUserRepository.findAll();
            for (com.project.kfpcl_exports.buyer.model.User buyer : buyers) {
                try {
                    Notification notif = Notification.builder()
                            .user(buyer)
                            .type(NotificationType.GENERAL)
                            .title(title)
                            .message(message)
                            .isRead(false)
                            .createdAt(now)
                            .build();
                    notificationRepository.save(notif);
                } catch (Exception e) {
                    log.error("Failed to save notification for buyer {}: {}", buyer.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving buyers for notification dispatch: {}", e.getMessage());
        }

        // 2. Dispatch FCM Push Notification to all registered customer device tokens
        try {
            List<DeviceToken> customerTokens = deviceTokenRepository.findByUserType("CUSTOMER");
            for (DeviceToken dt : customerTokens) {
                try {
                    fcmTokenService.sendPushNotification(dt.getToken(), title, message);
                } catch (Exception e) {
                    log.warn("Failed to push FCM to customer device token {}: {}", dt.getToken(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error sending FCM push notifications: {}", e.getMessage());
        }
    }

    @PostMapping({"/save-token", "/api/save-token"})
    public ResponseEntity<Map<String, Object>> saveToken(@RequestBody Map<String, String> payload) {
        String tokenStr = payload.get("token");
        String userType = payload.getOrDefault("userType", "CUSTOMER");

        if (tokenStr != null && !tokenStr.isBlank()) {
            Optional<DeviceToken> existingOpt = deviceTokenRepository.findByToken(tokenStr);
            if (existingOpt.isEmpty()) {
                deviceTokenRepository.save(DeviceToken.builder()
                        .token(tokenStr)
                        .userType(userType)
                        .build());
            }
            return ResponseEntity.ok(Map.of("message", "Device token registered", "success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Token is required", "success", false));
    }
}

