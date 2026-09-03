package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.dto.NotificationRequest;
import com.project.kfpcl_exports.admin.model.DeviceToken;
import com.project.kfpcl_exports.admin.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController("adminNotificationController")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;

    @PostMapping("/notifications/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(Map.of(
                "message", "Notification sent successfully",
                "title", request.getTitle() != null ? request.getTitle() : "",
                "recipientType", request.getRecipientType() != null ? request.getRecipientType() : "ALL",
                "success", true
        ));
    }

    @PostMapping("/notifications/send-to-customers")
    public ResponseEntity<Map<String, Object>> sendToCustomers(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to all active customers",
                "title", request.getTitle() != null ? request.getTitle() : "",
                "success", true
        ));
    }

    @PostMapping("/notifications/send-to-delivery")
    public ResponseEntity<Map<String, Object>> sendToDelivery(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to all delivery personnel",
                "title", request.getTitle() != null ? request.getTitle() : "",
                "success", true
        ));
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
