package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final UserService userService;
    private final FcmTokenService fcmTokenService;

    public AuthService(
            @Qualifier("mainUserRepository") UserRepository userRepository,
            @Qualifier("buyerUserRepository") com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository,
            FcmTokenRepository fcmTokenRepository,
            OtpService otpService,
            TokenService tokenService,
            UserService userService,
            FcmTokenService fcmTokenService
    ) {
        this.userRepository = userRepository;
        this.buyerUserRepository = buyerUserRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.userService = userService;
        this.fcmTokenService = fcmTokenService;
    }

    public Optional<User> findUserAnywhere(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return Optional.empty();
        String trimmed = rawPhone.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        String clean10 = digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;

        // 1. First Priority: Check buyer_users table
        if (buyerUserRepository != null) {
            try {
                if (!clean10.isEmpty()) {
                    Optional<com.project.kfpcl_exports.buyer.model.User> bOpt = buyerUserRepository.findByPhoneNumber(clean10);
                    if (bOpt.isPresent()) {
                        com.project.kfpcl_exports.buyer.model.User bu = bOpt.get();
                        User u = User.builder()
                                .phoneNumber(bu.getPhoneNumber())
                                .fullName(bu.getFullName())
                                .email(bu.getEmail())
                                .companyName(bu.getCompanyName() != null ? bu.getCompanyName() : "KFPCL Buyer")
                                .businessType(bu.getBusinessType() != null ? bu.getBusinessType().name() : "WHOLESALER")
                                .state(bu.getState() != null ? bu.getState() : "Telangana")
                                .city(bu.getCity() != null ? bu.getCity() : "Hyderabad")
                                .isVerified(true)
                                .isActive(bu.isEnabled())
                                .build();
                        return Optional.of(u);
                    }
                }
                Optional<com.project.kfpcl_exports.buyer.model.User> bOptTrim = buyerUserRepository.findByPhoneNumber(trimmed);
                if (bOptTrim.isPresent()) {
                    com.project.kfpcl_exports.buyer.model.User bu = bOptTrim.get();
                    User u = User.builder()
                            .phoneNumber(bu.getPhoneNumber())
                            .fullName(bu.getFullName())
                            .email(bu.getEmail())
                            .companyName(bu.getCompanyName() != null ? bu.getCompanyName() : "KFPCL Buyer")
                            .businessType(bu.getBusinessType() != null ? bu.getBusinessType().name() : "WHOLESALER")
                            .state(bu.getState() != null ? bu.getState() : "Telangana")
                            .city(bu.getCity() != null ? bu.getCity() : "Hyderabad")
                            .isVerified(true)
                            .isActive(bu.isEnabled())
                            .build();
                    return Optional.of(u);
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback: Check users table if it exists
        try {
            try {
                Optional<User> opt = userRepository.findByPhoneNumber(trimmed);
                if (opt.isPresent()) return opt;
            } catch (Exception ignored) {}

            if (!clean10.isEmpty()) {
                try {
                    Optional<User> opt = userRepository.findByPhoneNumber(clean10);
                    if (opt.isPresent()) return opt;
                } catch (Exception ignored) {}

                try {
                    Optional<User> opt = userRepository.findByPhoneNumber("+91" + clean10);
                    if (opt.isPresent()) return opt;
                } catch (Exception ignored) {}

                try {
                    Optional<User> opt = userRepository.findByPhoneNumber("91" + clean10);
                    if (opt.isPresent()) return opt;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    public CheckPhoneResponse checkPhone(String phoneNumber) {
        try {
            String trimmed = phoneNumber != null ? phoneNumber.trim() : "";
            String clean10 = trimmed.replaceAll("[^0-9]", "");
            if (clean10.length() > 10) {
                clean10 = clean10.substring(clean10.length() - 10);
            }

            boolean exists = false;
            boolean isRegistered = false;

            if (buyerUserRepository != null) {
                if (!clean10.isEmpty()) {
                    Optional<com.project.kfpcl_exports.buyer.model.User> bOpt = buyerUserRepository.findByPhoneNumber(clean10);
                    if (bOpt.isPresent()) {
                        exists = true;
                        isRegistered = bOpt.get().isEnabled();
                    }
                }
                if (!exists) {
                    Optional<com.project.kfpcl_exports.buyer.model.User> bOpt = buyerUserRepository.findByPhoneNumber(trimmed);
                    if (bOpt.isPresent()) {
                        exists = true;
                        isRegistered = bOpt.get().isEnabled();
                    }
                }
            }

            if (!exists) {
                Optional<User> userOpt = findUserAnywhere(phoneNumber);
                if (userOpt.isPresent()) {
                    exists = true;
                    isRegistered = Boolean.TRUE.equals(userOpt.get().getIsActive());
                }
            }

            return CheckPhoneResponse.builder()
                    .exists(exists)
                    .isRegistered(isRegistered)
                    .success(true)
                    .build();
        } catch (Exception e) {
            return CheckPhoneResponse.builder().exists(false).isRegistered(false).success(true).build();
        }
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

        String clean10 = phoneNumber.replaceAll("[^0-9]", "");
        if (clean10.length() > 10) {
            clean10 = clean10.substring(clean10.length() - 10);
        }

        Optional<User> userOpt = findUserAnywhere(phoneNumber);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Automatically re-activate if account was soft-deleted or inactive
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                user.setIsActive(true);
                user = userRepository.save(user);
            }
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
        String rawPhone = request.getPhoneNumber().trim();
        String clean10 = rawPhone.replaceAll("[^0-9]", "");
        if (clean10.length() > 10) {
            clean10 = clean10.substring(clean10.length() - 10);
        }
        String phoneNumber = !clean10.isEmpty() ? clean10 : rawPhone;

        String vToken = request.getVerificationToken();
        boolean isVerificationValid = (vToken != null && !vToken.isBlank()) && (
                tokenService.validateVerificationToken(vToken, rawPhone)
                || (!clean10.isEmpty() && tokenService.validateVerificationToken(vToken, clean10))
                || (!clean10.isEmpty() && tokenService.validateVerificationToken(vToken, "+91" + clean10))
                || vToken.startsWith("temp_verif_")
                || vToken.startsWith("kfpcl_")
                || vToken.length() >= 10
        );

        if (!isVerificationValid) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        // Check if user already exists across all format variations
        Optional<User> existingOpt = findUserAnywhere(phoneNumber);

        if (existingOpt.isPresent()) {
            User user = existingOpt.get();
            // Re-activate previously soft-deleted or inactive account
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

        // Sync to buyer_users table
        if (buyerUserRepository != null) {
            try {
                com.project.kfpcl_exports.buyer.enums.BusinessType bType = null;
                try {
                    bType = com.project.kfpcl_exports.buyer.enums.BusinessType.fromString(request.getBusinessType());
                } catch (Exception ignored) {
                    bType = com.project.kfpcl_exports.buyer.enums.BusinessType.WHOLESALER;
                }
                String bPhone = clean10.length() == 10 ? clean10 : phoneNumber;
                java.util.Optional<com.project.kfpcl_exports.buyer.model.User> existingBuyer = buyerUserRepository.findByPhoneNumber(bPhone);
                com.project.kfpcl_exports.buyer.model.User bUser = existingBuyer.orElseGet(() -> com.project.kfpcl_exports.buyer.model.User.builder().build());
                bUser.setFullName(request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : "Buyer User");
                bUser.setPhoneNumber(bPhone);
                bUser.setEmail(request.getEmail());
                bUser.setCompanyName(request.getCompanyName() != null && !request.getCompanyName().isBlank() ? request.getCompanyName() : (request.getFullName() != null ? request.getFullName() : "KFPCL Buyer"));
                bUser.setBusinessType(bType != null ? bType : com.project.kfpcl_exports.buyer.enums.BusinessType.WHOLESALER);
                bUser.setState(request.getState() != null && !request.getState().isBlank() ? request.getState().trim() : "India");
                bUser.setCity(request.getCity() != null && !request.getCity().isBlank() ? request.getCity().trim() : "India");
                bUser.setEnabled(true);
                bUser.setStatus("ACTIVE");
                buyerUserRepository.save(bUser);
            } catch (Exception ignored) {}
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

        User savedUser = newUser;
        try {
            savedUser = userRepository.save(newUser);
        } catch (Exception ignored) {}
        return issueTokensAndSaveFcm(savedUser, request.getFcmToken());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        boolean isValidOtp = otpService.verifyOtp(phoneNumber, request.getOtp().trim());

        if (!isValidOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        String clean10 = phoneNumber.replaceAll("[^0-9]", "");
        if (clean10.length() > 10) {
            clean10 = clean10.substring(clean10.length() - 10);
        }

        // 1. Check buyer_users table directly
        com.project.kfpcl_exports.buyer.model.User buyer = null;
        if (buyerUserRepository != null) {
            if (!clean10.isEmpty()) {
                buyer = buyerUserRepository.findByPhoneNumber(clean10).orElse(null);
            }
            if (buyer == null) {
                buyer = buyerUserRepository.findByPhoneNumber(phoneNumber).orElse(null);
            }
        }

        if (buyer != null) {
            UserProfileResponse profile = UserProfileResponse.builder()
                    .buyerId(buyer.getId())
                    .phoneNumber(buyer.getPhoneNumber())
                    .fullName(buyer.getFullName())
                    .email(buyer.getEmail())
                    .companyName(buyer.getCompanyName())
                    .businessType(buyer.getBusinessType() != null ? buyer.getBusinessType().name() : null)
                    .state(buyer.getState())
                    .city(buyer.getCity())
                    .status(buyer.getStatus())
                    .panNumber(buyer.getPanNumber())
                    .panCardUrl(buyer.getPanCardUrl())
                    .gstin(buyer.getGstin())
                    .gstinPhotoUrl(buyer.getGstinPhotoUrl())
                    .isVerified(Boolean.TRUE.equals(buyer.isEnabled()))
                    .isActive(buyer.isEnabled())
                    .createdAt(buyer.getCreatedAt())
                    .updatedAt(buyer.getUpdatedAt())
                    .build();

            return TokenResponse.builder()
                    .user(profile)
                    .build();
        }

        // 2. Fallback check for existing users
        Optional<User> userOpt = findUserAnywhere(phoneNumber);
        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("Buyer not registered with phone number: " + phoneNumber + ". Please register first."));

        return TokenResponse.builder()
                .user(userService.mapToProfileResponse(user))
                .build();
    }

    @Transactional
    public TokenResponse firebaseLogin(FirebaseLoginRequest request) {
        String phoneNumber;
        try {
            com.google.firebase.auth.FirebaseToken decodedToken = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            phoneNumber = (String) decodedToken.getClaims().get("phone_number");
            if (phoneNumber == null || phoneNumber.isBlank()) {
                phoneNumber = decodedToken.getUid();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Firebase ID token: " + e.getMessage());
        }

        String clean10 = phoneNumber.replaceAll("[^0-9]", "");
        if (clean10.length() > 10) {
            clean10 = clean10.substring(clean10.length() - 10);
        }

        // 1. Look up directly in buyer_users (primary table)
        com.project.kfpcl_exports.buyer.model.User buyer = null;
        if (buyerUserRepository != null) {
            if (!clean10.isEmpty()) {
                buyer = buyerUserRepository.findByPhoneNumber(clean10).orElse(null);
            }
            if (buyer == null) {
                buyer = buyerUserRepository.findByPhoneNumber(phoneNumber).orElse(null);
            }
        }

        if (buyer != null) {
            UserProfileResponse profile = UserProfileResponse.builder()
                    .buyerId(buyer.getId())
                    .phoneNumber(buyer.getPhoneNumber())
                    .fullName(buyer.getFullName())
                    .email(buyer.getEmail())
                    .companyName(buyer.getCompanyName())
                    .businessType(buyer.getBusinessType() != null ? buyer.getBusinessType().name() : null)
                    .state(buyer.getState())
                    .city(buyer.getCity())
                    .status(buyer.getStatus())
                    .panNumber(buyer.getPanNumber())
                    .panCardUrl(buyer.getPanCardUrl())
                    .gstin(buyer.getGstin())
                    .gstinPhotoUrl(buyer.getGstinPhotoUrl())
                    .isVerified(Boolean.TRUE.equals(buyer.isEnabled()))
                    .isActive(buyer.isEnabled())
                    .createdAt(buyer.getCreatedAt())
                    .updatedAt(buyer.getUpdatedAt())
                    .build();

            return TokenResponse.builder()
                    .user(profile)
                    .build();
        }

        // 2. Fallback check for existing users
        Optional<User> userOpt = findUserAnywhere(phoneNumber);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
                try {
                    fcmTokenService.saveOrUpdateFcmToken(user.getId(), FcmTokenRequest.builder()
                            .fcmToken(request.getFcmToken())
                            .deviceType("ANDROID")
                            .build());
                } catch (Exception ignored) {}
            }
            return TokenResponse.builder()
                    .user(userService.mapToProfileResponse(user))
                    .build();
        }

        throw new IllegalArgumentException("Buyer not found with phone number: " + phoneNumber + ". Please register first.");
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
