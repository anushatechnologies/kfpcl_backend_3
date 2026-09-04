package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.GenericResponse;
import com.project.kfpcl_exports.dto.AuthDTOs.ProfileUpdateRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.UserProfileResponse;
import com.project.kfpcl_exports.security.UserPrincipal;
import com.project.kfpcl_exports.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUserId()));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUserId(), request));
    }

    @DeleteMapping
    public ResponseEntity<GenericResponse> deleteProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        userService.softDeleteProfile(principal.getUserId(), principal.getAccessToken(), refreshToken);
        return ResponseEntity.ok(GenericResponse.builder()
                .success(true)
                .message("Buyer profile account deactivated successfully")
                .build());
    }
}
