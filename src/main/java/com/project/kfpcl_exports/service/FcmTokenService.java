package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.FcmTokenRequest;
import com.project.kfpcl_exports.model.FcmToken;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveOrUpdateFcmToken(Long userId, FcmTokenRequest request) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Optional<FcmToken> existingOpt = fcmTokenRepository.findByFcmToken(request.getFcmToken());
        if (existingOpt.isPresent()) {
            FcmToken existing = existingOpt.get();
            if (user != null) {
                existing.setUser(user);
            }
            if (request.getDeviceType() != null) {
                existing.setDeviceType(request.getDeviceType());
            }
            fcmTokenRepository.save(existing);
        } else {
            FcmToken newToken = FcmToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "ANDROID")
                    .build();
            fcmTokenRepository.save(newToken);
        }
    }
}
