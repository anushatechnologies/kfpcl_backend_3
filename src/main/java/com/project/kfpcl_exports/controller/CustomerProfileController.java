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
@RequestMapping({"/api/customer/profile", "/customer/profile"})
@RequiredArgsConstructor
public class CustomerProfileController {

    private final UserService userService;
    private final com.project.kfpcl_exports.repository.UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            jakarta.servlet.http.HttpServletRequest request) {
        if (principal != null) {
            return ResponseEntity.ok(userService.getProfile(principal.getBuyerId(), principal.getUserId()));
        }

        // Fallback to headers (X-Phone-Number, X-User-Email, X-Customer-Id)
        if (request != null) {
            String phone = request.getHeader("X-Phone-Number");
            if (phone != null && !phone.isBlank()) {
                String clean = phone.replaceAll("[^0-9]", "");
                if (clean.length() > 10) clean = clean.substring(clean.length() - 10);
                var userOpt = userRepository.findByPhoneNumber(clean);
                if (userOpt.isPresent()) {
                    return ResponseEntity.ok(userService.mapToProfileResponse(userOpt.get()));
                }
            }
            String email = request.getHeader("X-User-Email");
            if (email != null && !email.isBlank()) {
                var userOpt = userRepository.findByEmail(email.trim());
                if (userOpt.isPresent()) {
                    return ResponseEntity.ok(userService.mapToProfileResponse(userOpt.get()));
                }
            }
        }

        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .body(GenericResponse.builder().success(false).message("Unauthorized: please provide valid token or auth headers").build());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(GenericResponse.builder().success(false).message("Unauthorized").build());
        }
        return ResponseEntity.ok(userService.updateProfile(principal.getBuyerId(), principal.getUserId(), request));
    }

    @DeleteMapping
    public ResponseEntity<GenericResponse> deleteProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        if (principal != null) {
            userService.softDeleteProfile(principal.getBuyerId(), principal.getUserId(), principal.getAccessToken(), refreshToken);
        }
        return ResponseEntity.ok(GenericResponse.builder()
                .success(true)
                .message("Buyer profile account deactivated successfully")
                .build());
    }
}
