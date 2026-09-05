package com.project.kfpcl_exports.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.project.kfpcl_exports.dto.AuthDTOs.FcmTokenRequest;
import com.project.kfpcl_exports.model.FcmToken;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public FcmTokenService(FcmTokenRepository fcmTokenRepository, @Qualifier("mainUserRepository") UserRepository userRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
    }

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

    public String sendPushNotification(String targetToken, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("[FCM SERVICE] Successfully sent message: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("[FCM SERVICE ERROR] Failed to send push notification: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("Firebase Messaging Error: " + e.getMessage(), e);
        }
    }
}
