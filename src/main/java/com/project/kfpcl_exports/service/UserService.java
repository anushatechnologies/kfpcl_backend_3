package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Autowired(required = false)
    private com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository;

    public UserService(@Qualifier("mainUserRepository") UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public UserProfileResponse getProfile(Long userId) {
        return getProfile(null, userId);
    }

    public UserProfileResponse getProfile(String buyerId, Long userId) {
        if (buyerId != null && buyerUserRepository != null) {
            var bOpt = buyerUserRepository.findById(buyerId);
            if (bOpt.isPresent()) {
                return mapBuyerToProfileResponse(bOpt.get());
            }
        }
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .filter(User::getIsActive)
                    .orElseThrow(() -> new IllegalArgumentException("User not found or account inactive"));
            return mapToProfileResponse(user);
        }
        throw new IllegalArgumentException("User not found or account inactive");
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        return updateProfile(null, userId, request);
    }

    @Transactional
    public UserProfileResponse updateProfile(String buyerId, Long userId, ProfileUpdateRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().length() < 3) {
            throw new IllegalArgumentException("Full name must be at least 3 characters");
        }

        if (buyerId != null && buyerUserRepository != null) {
            var bOpt = buyerUserRepository.findById(buyerId);
            if (bOpt.isPresent()) {
                var b = bOpt.get();
                b.setFullName(request.getFullName().trim());
                b.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : b.getEmail());
                b.setCompanyName(request.getCompanyName().trim());
                try {
                    b.setBusinessType(com.project.kfpcl_exports.buyer.enums.BusinessType.fromString(request.getBusinessType()));
                } catch (Exception ignored) {}
                b.setState(request.getState().trim());
                b.setCity(request.getCity().trim());
                var saved = buyerUserRepository.save(b);
                return mapBuyerToProfileResponse(saved);
            }
        }

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .filter(User::getIsActive)
                    .orElseThrow(() -> new IllegalArgumentException("User not found or account inactive"));

            user.setFullName(request.getFullName().trim());
            user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
            user.setCompanyName(request.getCompanyName().trim());
            user.setBusinessType(request.getBusinessType().trim());
            user.setState(request.getState().trim());
            user.setCity(request.getCity().trim());

            User updatedUser = userRepository.save(user);
            return mapToProfileResponse(updatedUser);
        }

        throw new IllegalArgumentException("User not found or account inactive");
    }

    @Transactional
    public void softDeleteProfile(Long userId, String accessToken, String refreshToken) {
        softDeleteProfile(null, userId, accessToken, refreshToken);
    }

    @Transactional
    public void softDeleteProfile(String buyerId, Long userId, String accessToken, String refreshToken) {
        if (buyerId != null && buyerUserRepository != null) {
            var bOpt = buyerUserRepository.findById(buyerId);
            if (bOpt.isPresent()) {
                var b = bOpt.get();
                b.setEnabled(false);
                b.setStatus("DEACTIVATED");
                buyerUserRepository.save(b);
                tokenService.invalidateSession(accessToken, refreshToken);
                return;
            }
        }

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            user.setIsActive(false);
            userRepository.save(user);
            tokenService.invalidateSession(accessToken, refreshToken);
        }
    }

    public UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .businessType(user.getBusinessType())
                .state(user.getState())
                .city(user.getCity())
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserProfileResponse mapBuyerToProfileResponse(com.project.kfpcl_exports.buyer.model.User buyer) {
        return UserProfileResponse.builder()
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
    }
}
