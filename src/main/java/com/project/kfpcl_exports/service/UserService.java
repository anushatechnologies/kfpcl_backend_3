package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new IllegalArgumentException("User not found or account inactive"));
        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new IllegalArgumentException("User not found or account inactive"));

        if (request.getFullName() == null || request.getFullName().trim().length() < 3) {
            throw new IllegalArgumentException("Full name must be at least 3 characters");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        user.setCompanyName(request.getCompanyName().trim());
        user.setBusinessType(request.getBusinessType().trim());
        user.setState(request.getState().trim());
        user.setCity(request.getCity().trim());

        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(updatedUser);
    }

    @Transactional
    public void softDeleteProfile(Long userId, String accessToken, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        // Invalidate active session tokens
        tokenService.invalidateSession(accessToken, refreshToken);
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
}
