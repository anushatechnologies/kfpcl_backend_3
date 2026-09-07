package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.project.kfpcl_exports.dto.AuthDTOs.FcmTokenRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.TestNotificationRequest;
import com.project.kfpcl_exports.model.FcmToken;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.FcmTokenRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import com.project.kfpcl_exports.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FirebaseNotificationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    @Qualifier("mainUserRepository")
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify FirebaseApp is properly initialized from credentials")
    void testFirebaseAppInitialized() {
        assertFalse(FirebaseApp.getApps().isEmpty(), "FirebaseApp should be initialized with provided credentials");
        assertNotNull(FirebaseApp.getInstance(), "Default FirebaseApp instance should not be null");
        assertEquals("[DEFAULT]", FirebaseApp.getInstance().getName());
    }

    @Test
    @DisplayName("POST /api/save-token - Save FCM Push Token for authenticated user")
    void testSaveFcmTokenAuthenticated() throws Exception {
        String testPhone = "9888877771";
        User user = userRepository.findByPhoneNumber(testPhone)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .phoneNumber(testPhone)
                            .fullName("Test Buyer")
                            .companyName("Test Agro Ltd")
                            .businessType("EXPORTER")
                            .state("Telangana")
                            .city("Hyderabad")
                            .isVerified(true)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        String authToken = tokenService.createAccessToken(user.getId(), testPhone);

        FcmTokenRequest request = FcmTokenRequest.builder()
                .fcmToken("test-fcm-token-unique-987654")
                .deviceType("ANDROID")
                .build();

        mockMvc.perform(post("/api/save-token")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("FCM push token saved successfully"));

        Optional<FcmToken> tokenOpt = fcmTokenRepository.findByFcmToken("test-fcm-token-unique-987654");
        assertTrue(tokenOpt.isPresent(), "FCM token should be persisted in the database");
        assertNotNull(tokenOpt.get().getUser());
        assertEquals(user.getId(), tokenOpt.get().getUser().getId());
    }

    @Test
    @DisplayName("POST /api/save-token - Reject unauthenticated save token call")
    void testSaveFcmTokenUnauthenticated() throws Exception {
        FcmTokenRequest request = FcmTokenRequest.builder()
                .fcmToken("unauth-token-12345")
                .deviceType("IOS")
                .build();

        mockMvc.perform(post("/api/save-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/fcm/send-test-notification - Public endpoint validates request and handles notification dispatch")
    void testSendTestNotificationEndpoint() throws Exception {
        TestNotificationRequest request = new TestNotificationRequest(
                "mock-device-fcm-token-test",
                "KFPCL Harvest Alert",
                "Your organic mango shipment has arrived."
        );

        // Should return either 200 (if FCM succeeds) or 400 with a detailed error message from Firebase
        mockMvc.perform(post("/api/fcm/send-test-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 400, "Expected 200 or 400 from Firebase endpoint, got " + status);
                });
    }
}
