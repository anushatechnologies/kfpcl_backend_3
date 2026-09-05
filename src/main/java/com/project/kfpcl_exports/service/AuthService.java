package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final UserService userService;
    private final FcmTokenService fcmTokenService;

    public AuthService(
            @Qualifier("mainUserRepository") UserRepository userRepository,
            FcmTokenRepository fcmTokenRepository,
            OtpService otpService,
            TokenService tokenService,
            UserService userService,
            FcmTokenService fcmTokenService
    ) {
        this.userRepository = userRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.userService = userService;
        this.fcmTokenService = fcmTokenService;
    }

    public CheckPhoneResponse checkPhone(String phoneNumber) {
        boolean exists = userRepository.existsByPhoneNumberAndIsActiveTrue(phoneNumber);
        return CheckPhoneResponse.builder().exists(exists).build();
    }

    public SendOtpResponse sendOtp(SendOtpRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        otpService.generateAndSendOtp(phoneNumber);

        return SendOtpResponse.builder()
                .success(true)
                .message("OTP sent successfully")
                .expiresInSeconds(otpService.getOtpTtlSeconds())
                .build();
    }

    public SendOtpResponse resendOtp(ResendOtpRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        otpService.generateAndSendOtp(phoneNumber);

        return SendOtpResponse.builder()
                .success(true)
                .message("Replacement OTP sent successfully")
                .expiresInSeconds(otpService.getOtpTtlSeconds())
                .build();
    }

    public OtpDebugResponse getOtpForPhone(String phoneNumber) {
        OtpService.OtpData data = otpService.getActiveOtpData(phoneNumber.trim());
        if (data == null) {
            throw new IllegalArgumentException("No active OTP found for phone number: " + phoneNumber + ". Please request /send-otp first.");
        }
        return OtpDebugResponse.builder()
                .phoneNumber(phoneNumber)
                .otp(data.getOtp())
                .build();
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        boolean isValidOtp = otpService.verifyOtp(phoneNumber, request.getOtp().trim());

        if (!isValidOtp) {
            return VerifyOtpResponse.builder()
                    .success(false)
                    .verified(false)
                    .isRegistered(false)
                    .build();
        }

        Optional<User> userOpt = userRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
                fcmTokenService.saveOrUpdateFcmToken(user.getId(), FcmTokenRequest.builder()
                        .fcmToken(request.getFcmToken())
                        .deviceType("ANDROID")
                        .build());
            }
            String accessToken = tokenService.createAccessToken(user.getId(), user.getPhoneNumber());
            String refreshToken = tokenService.createRefreshToken(user.getId(), user.getPhoneNumber());

            return VerifyOtpResponse.builder()
                    .success(true)
                    .verified(true)
                    .isRegistered(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userService.mapToProfileResponse(user))
                    .build();
        } else {
            String verificationToken = tokenService.createVerificationToken(phoneNumber);
            return VerifyOtpResponse.builder()
                    .success(true)
                    .verified(true)
                    .isRegistered(false)
                    .verificationToken(verificationToken)
                    .build();
        }
    }

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();

        boolean isVerificationValid = tokenService.validateVerificationToken(
                request.getVerificationToken(), phoneNumber
        );

        if (!isVerificationValid) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            Optional<User> existingOpt = userRepository.findByPhoneNumber(phoneNumber);
            if (existingOpt.isPresent() && Boolean.FALSE.equals(existingOpt.get().getIsActive())) {
                // Re-activate previously soft-deleted account
                User user = existingOpt.get();
                user.setFullName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setCompanyName(request.getCompanyName());
                user.setBusinessType(request.getBusinessType());
                user.setState(request.getState());
                user.setCity(request.getCity());
                user.setIsActive(true);
                user.setIsVerified(true);
                User saved = userRepository.save(user);

                return issueTokensAndSaveFcm(saved, request.getFcmToken());
            }
            throw new IllegalStateException("User with this phone number is already registered.");
        }

        User newUser = User.builder()
                .phoneNumber(phoneNumber)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .companyName(request.getCompanyName())
                .businessType(request.getBusinessType())
                .state(request.getState())
                .city(request.getCity())
                .isVerified(true)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);
        return issueTokensAndSaveFcm(savedUser, request.getFcmToken());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        boolean isValidOtp = otpService.verifyOtp(phoneNumber, request.getOtp().trim());

        if (!isValidOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not registered or account inactive"));

        return issueTokensAndSaveFcm(user, request.getFcmToken());
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        TokenService.RefreshTokenData data = tokenService.validateRefreshToken(refreshToken);

        if (data == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(data.getUserId())
                .filter(User::getIsActive)
                .orElseThrow(() -> new IllegalArgumentException("User not found or account inactive"));

        String newAccessToken = tokenService.rotateAccessToken(refreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .user(userService.mapToProfileResponse(user))
                .build();
    }

    @Transactional
    public GenericResponse logout(Long userId, String accessToken, LogoutRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        tokenService.invalidateSession(accessToken, refreshToken);

        if (userId != null) {
            fcmTokenRepository.deleteByUserId(userId);
        }

        return GenericResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build();
    }

    private TokenResponse issueTokensAndSaveFcm(User user, String fcmToken) {
        String accessToken = tokenService.createAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getPhoneNumber());

        if (fcmToken != null && !fcmToken.isBlank()) {
            fcmTokenService.saveOrUpdateFcmToken(user.getId(), FcmTokenRequest.builder()
                    .fcmToken(fcmToken)
                    .deviceType("ANDROID")
                    .build());
        }

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userService.mapToProfileResponse(user))
                .build();
    }
}
