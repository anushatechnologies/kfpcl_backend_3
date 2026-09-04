package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.security.UserPrincipal;
import com.project.kfpcl_exports.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-phone/{phone}")
    public ResponseEntity<CheckPhoneResponse> checkPhone(@PathVariable("phone") String phone) {
        return ResponseEntity.ok(authService.checkPhone(phone));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(authService.sendOtp(request));
    }

    @GetMapping("/get-otp/{phone}")
    public ResponseEntity<OtpDebugResponse> getOtp(@PathVariable("phone") String phone) {
        return ResponseEntity.ok(authService.getOtpForPhone(phone));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<SendOtpResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<GenericResponse> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) LogoutRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        String accessToken = principal != null ? principal.getAccessToken() : null;
        return ResponseEntity.ok(authService.logout(userId, accessToken, request));
    }
}
