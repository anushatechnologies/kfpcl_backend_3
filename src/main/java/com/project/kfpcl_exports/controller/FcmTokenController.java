package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.FcmTokenRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.GenericResponse;
import com.project.kfpcl_exports.dto.AuthDTOs.TestNotificationRequest;
import com.project.kfpcl_exports.security.UserPrincipal;
import com.project.kfpcl_exports.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/api/save-token")
    public ResponseEntity<GenericResponse> saveToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FcmTokenRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        fcmTokenService.saveOrUpdateFcmToken(userId, request);
        return ResponseEntity.ok(GenericResponse.builder()
                .success(true)
                .message("FCM push token saved successfully")
                .build());
    }

    @PostMapping("/api/fcm/send-test-notification")
    public ResponseEntity<GenericResponse> sendTestNotification(
            @Valid @RequestBody TestNotificationRequest request) {
        String title = request.getTitle() != null ? request.getTitle() : "KFPCL Test Notification";
        String body = request.getBody() != null ? request.getBody() : "Hello! This is a test Firebase push notification from KFPCL Backend.";

        try {
            String messageId = fcmTokenService.sendPushNotification(request.getFcmToken(), title, body);
            return ResponseEntity.ok(GenericResponse.builder()
                    .success(true)
                    .message("Firebase notification dispatched successfully. Message ID: " + messageId)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GenericResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
