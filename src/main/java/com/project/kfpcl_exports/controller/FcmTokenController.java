package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.FcmTokenRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.GenericResponse;
import com.project.kfpcl_exports.security.UserPrincipal;
import com.project.kfpcl_exports.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/save-token")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
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
}
