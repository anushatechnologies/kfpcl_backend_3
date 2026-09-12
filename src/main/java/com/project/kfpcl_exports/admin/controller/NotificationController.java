package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.dto.NotificationRequest;
import com.project.kfpcl_exports.admin.model.DeviceToken;
import com.project.kfpcl_exports.admin.repository.DeviceTokenRepository;
import com.project.kfpcl_exports.buyer.enums.NotificationType;
import com.project.kfpcl_exports.buyer.model.Notification;
import com.project.kfpcl_exports.buyer.repository.NotificationRepository;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import com.project.kfpcl_exports.model.FcmToken;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.service.FcmTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController("adminNotificationController")
@RequestMapping("/api/admin")
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository buyerUserRepository;
    private final FcmTokenService fcmTokenService;
    private final FcmTokenRepository fcmTokenRepository;

    public NotificationController(
            DeviceTokenRepository deviceTokenRepository,
            NotificationRepository notificationRepository,
            @Qualifier("buyerUserRepository") UserRepository buyerUserRepository,
            FcmTokenService fcmTokenService,
            @Autowired(required = false) FcmTokenRepository fcmTokenRepository
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
        this.buyerUserRepository = buyerUserRepository;
        this.fcmTokenService = fcmTokenService;
        this.fcmTokenRepository = fcmTokenRepository;
    }

    @GetMapping({"/notifications", "/notifications/all"})
    public ResponseEntity<Map<String, Object>> getAdminNotifications() {
        try {
            com.project.kfpcl_exports.buyer.model.User admin = buyerUserRepository.findByEmail("admin@kfpcl.com").orElse(null);
            List<Notification> list;
            if (admin != null) {
                list = notificationRepository.findByUserOrderByCreatedAtDesc(admin);
            } else {
                list = notificationRepository.findAll().stream()
                        .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notifications", list,
                    "unreadCount", list.stream().filter(n -> !n.isRead()).count()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "notifications", Collections.emptyList(), "unreadCount", 0));
        }
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Object>> getAdminUnreadCount() {
        try {
            com.project.kfpcl_exports.buyer.model.User admin = buyerUserRepository.findByEmail("admin@kfpcl.com").orElse(null);
            long count = 0;
            if (admin != null) {
                count = notificationRepository.countByUserAndIsReadFalse(admin);
            } else {
                count = notificationRepository.findAll().stream().filter(n -> !n.isRead()).count();
            }
            return ResponseEntity.ok(Map.of("success", true, "unreadCount", count, "count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "unreadCount", 0, "count", 0));
        }
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> markAdminNotificationRead(@PathVariable Long id) {
        try {
            notificationRepository.findById(id).ifPresent(n -> {
                n.setRead(true);
                notificationRepository.save(n);
            });
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification updated"));
        }
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

        List<DeviceToken> deliveryTokens = deviceTokenRepository.findByUserTypeIgnoreCase("DELIVERY");
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

        // 1. Save in-app notification for all buyers in database
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

        // 2. Aggregate device push tokens from both device_tokens and fcm_tokens tables
        Set<String> uniqueTokens = new HashSet<>();

        try {
            List<DeviceToken> customerTokens = deviceTokenRepository.findByUserTypeIgnoreCase("CUSTOMER");
            for (DeviceToken dt : customerTokens) {
                if (dt.getToken() != null && !dt.getToken().isBlank()) {
                    uniqueTokens.add(dt.getToken().trim());
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching device_tokens for CUSTOMER: {}", e.getMessage());
        }

        if (fcmTokenRepository != null) {
            try {
                List<FcmToken> fcmTokens = fcmTokenRepository.findAll();
                for (FcmToken ft : fcmTokens) {
                    if (ft.getFcmToken() != null && !ft.getFcmToken().isBlank()) {
                        uniqueTokens.add(ft.getFcmToken().trim());
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching fcm_tokens: {}", e.getMessage());
            }
        }

        // 3. Dispatch FCM Push Notifications
        for (String token : uniqueTokens) {
            try {
                fcmTokenService.sendPushNotification(token, title, message);
            } catch (Exception e) {
                log.warn("Failed to push FCM to customer device token {}: {}", token, e.getMessage());
            }
        }
    }

    @PostMapping({"/save-token", "/api/save-token"})
    public ResponseEntity<Map<String, Object>> saveToken(@RequestBody Map<String, String> payload) {
        String tokenStr = payload.get("token");
        String rawUserType = payload.getOrDefault("userType", "CUSTOMER");
        String userType = rawUserType != null ? rawUserType.trim().toUpperCase() : "CUSTOMER";

        if (tokenStr != null && !tokenStr.isBlank()) {
            tokenStr = tokenStr.trim();
            Optional<DeviceToken> existingOpt = deviceTokenRepository.findByToken(tokenStr);
            if (existingOpt.isEmpty()) {
                deviceTokenRepository.save(DeviceToken.builder()
                        .token(tokenStr)
                        .userType(userType)
                        .build());
            } else {
                DeviceToken existing = existingOpt.get();
                existing.setUserType(userType);
                deviceTokenRepository.save(existing);
            }
            return ResponseEntity.ok(Map.of("message", "Device token registered", "success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Token is required", "success", false));
    }
}


