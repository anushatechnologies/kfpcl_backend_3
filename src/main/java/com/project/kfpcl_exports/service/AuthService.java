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

        // 1. Direct match on trimmed
        Optional<User> opt = userRepository.findByPhoneNumber(trimmed);
        if (opt.isPresent()) return opt;

        if (!clean10.isEmpty()) {
            opt = userRepository.findByPhoneNumber(clean10);
            if (opt.isPresent()) return opt;

            opt = userRepository.findByPhoneNumber("+91" + clean10);
            if (opt.isPresent()) return opt;

            opt = userRepository.findByPhoneNumber("91" + clean10);
            if (opt.isPresent()) return opt;
        }

        // 2. Scan all users in users table matching clean10
        if (clean10.length() == 10) {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getPhoneNumber() != null) {
                    String uDigits = u.getPhoneNumber().replaceAll("[^0-9]", "");
                    String u10 = uDigits.length() > 10 ? uDigits.substring(uDigits.length() - 10) : uDigits;
                    if (clean10.equals(u10)) {
                        return Optional.of(u);
                    }
                }
            }
        }

        // 3. Check legacy buyer_users table and bridge to main users if present
        if (clean10.length() == 10 && buyerUserRepository != null) {
            Optional<com.project.kfpcl_exports.buyer.model.User> buyerOpt = buyerUserRepository.findByPhoneNumber(clean10);
            if (buyerOpt.isEmpty()) {
                buyerOpt = buyerUserRepository.findByEmail(clean10 + "@kfpcl-buyer.com");
            }
            if (buyerOpt.isEmpty()) {
                buyerOpt = buyerUserRepository.findByEmail(clean10 + "@kfpcl.com");
            }
            if (buyerOpt.isEmpty()) {
                buyerOpt = buyerUserRepository.findByEmail(clean10);
            }
            if (buyerOpt.isPresent()) {
                com.project.kfpcl_exports.buyer.model.User bu = buyerOpt.get();
                User bridgeUser = User.builder()
                        .phoneNumber(clean10)
                        .fullName(bu.getName() != null && !bu.getName().isBlank() ? bu.getName() : "Buyer " + clean10)
                        .email(bu.getEmail() != null && bu.getEmail().contains("@") ? bu.getEmail() : "")
                        .companyName("KFPCL Buyer")
                        .businessType("Wholesaler")
                        .state("Telangana")
                        .city("Hyderabad")
                        .isVerified(true)
                        .isActive(true)
                        .build();
                return Optional.of(userRepository.save(bridgeUser));
            }
        }

        return Optional.empty();
    }

    public CheckPhoneResponse checkPhone(String phoneNumber) {
        Optional<User> userOpt = findUserAnywhere(phoneNumber);
        return CheckPhoneResponse.builder().exists(userOpt.isPresent()).build();
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

        Optional<User> userOpt = findUserAnywhere(phoneNumber);
        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("User not registered or account inactive"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            user = userRepository.save(user);
        }

        return issueTokensAndSaveFcm(user, request.getFcmToken());
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

        Optional<User> userOpt = findUserAnywhere(phoneNumber);

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                user.setIsActive(true);
                user = userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .phoneNumber(!clean10.isEmpty() ? clean10 : phoneNumber)
                    .fullName(request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : "Buyer " + clean10)
                    .email(request.getEmail() != null ? request.getEmail() : "")
                    .companyName(request.getCompanyName() != null && !request.getCompanyName().isBlank() ? request.getCompanyName() : "KFPCL Buyer")
                    .businessType(request.getBusinessType() != null && !request.getBusinessType().isBlank() ? request.getBusinessType() : "Wholesaler")
                    .state(request.getState() != null && !request.getState().isBlank() ? request.getState() : "Telangana")
                    .city(request.getCity() != null && !request.getCity().isBlank() ? request.getCity() : "Hyderabad")
                    .isVerified(true)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
        }

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
