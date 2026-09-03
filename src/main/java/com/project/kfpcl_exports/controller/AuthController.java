package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthResponse;
import com.project.kfpcl_exports.dto.LoginRequest;
import com.project.kfpcl_exports.dto.PasswordChangeRequest;
import com.project.kfpcl_exports.model.AdminUser;
import com.project.kfpcl_exports.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/adminpanel")
@RequiredArgsConstructor
public class AuthController {

    private final AdminUserRepository adminUserRepository;

    /**
     * Login - No JWT, No Token.
     * Validates email & password against MySQL and returns user info.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Optional<AdminUser> userOpt = adminUserRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            AdminUser user = userOpt.get();
            return ResponseEntity.ok(AuthResponse.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .message("Login successful")
                    .success(true)
                    .build());
        }
        return ResponseEntity.status(401).body(AuthResponse.builder()
                .message("Invalid email or password")
                .success(false)
                .build());
    }

    /**
     * Firebase-login compatible endpoint — no Firebase, just returns user info.
     */
    @PostMapping("/firebase-login")
    public ResponseEntity<AuthResponse> firebaseLogin(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "admin@kfpclexports.com");
        Optional<AdminUser> userOpt = adminUserRepository.findByEmail(email);
        AdminUser user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = adminUserRepository.save(AdminUser.builder()
                    .email(email)
                    .password("admin123")
                    .name("Admin User")
                    .role("ADMIN")
                    .build());
        }
        return ResponseEntity.ok(AuthResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .message("Login successful")
                .success(true)
                .build());
    }

    /**
     * Refresh — No token needed. Always returns success.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody(required = false) Map<String, String> payload) {
        return ResponseEntity.ok(Map.of(
                "message", "Session refreshed",
                "success", true
        ));
    }

    /**
     * Logout — No token to invalidate. Just returns success.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully", "success", true));
    }

    /**
     * Change Password — updates password in MySQL.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody PasswordChangeRequest request) {
        Optional<AdminUser> userOpt = adminUserRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            AdminUser user = userOpt.get();
            if (user.getPassword().equals(request.getOldPassword())) {
                user.setPassword(request.getNewPassword());
                adminUserRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Password changed successfully", "success", true));
            }
        }
        return ResponseEntity.status(400).body(Map.of("message", "Invalid credentials", "success", false));
    }

    /**
     * Prepare Password Reset — resets password directly in MySQL.
     */
    @PostMapping("/prepare-password-reset")
    public ResponseEntity<Map<String, Object>> preparePasswordReset(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "");
        String newPassword = payload.getOrDefault("newPassword", "admin123");
        Optional<AdminUser> userOpt = adminUserRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            AdminUser user = userOpt.get();
            user.setPassword(newPassword);
            adminUserRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully",
                    "email", email,
                    "success", true
            ));
        }
        return ResponseEntity.ok(Map.of(
                "message", "Password reset prepared (email not found in DB)",
                "email", email,
                "success", true
        ));
    }

    /**
     * Sync Firebase Password — no Firebase. Always returns success.
     */
    @PostMapping("/sync-firebase-password")
    public ResponseEntity<Map<String, Object>> syncFirebasePassword(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of(
                "message", "Password sync completed",
                "success", true
        ));
    }
}
