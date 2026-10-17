package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.AdminUser;
import com.project.kfpcl_exports.admin.repository.AdminUserRepository;
import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.repository.UserRepository;
import com.project.kfpcl_exports.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @Autowired
    @Qualifier("mainUserRepository")
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    @Qualifier("buyerUserRepository")
    private com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.project.kfpcl_exports.service.FirebaseAuthService firebaseAuthService;

    private static final String ADMIN_EMAIL = "admin@kfpclexports.com";

    @BeforeEach
    void setUp() {
        otpService.clearOtpStorage();

        // Seed admin user if not already present
        if (adminUserRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            adminUserRepository.save(AdminUser.builder()
                    .email(ADMIN_EMAIL)
                    .password("admin123")
                    .name("KFPCL Super Admin")
                    .role("ADMIN")
                    .build());
        }
    }

    // =========================================================================
    // 1. BUYER APP AUTHENTICATION APIS (/api/auth/**)
    // =========================================================================

    @Test
    @DisplayName("GET /api/auth/check-phone/{phone} - Check unregistered phone")
    void testCheckPhone() throws Exception {
        String unregisteredPhone = "9999900001";

        mockMvc.perform(get("/api/auth/check-phone/" + unregisteredPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/send-otp and GET /api/auth/get-otp/{phone} - Send and retrieve OTP")
    void testSendOtpAndGetOtp() throws Exception {
        String phone = "9110001112";

        // 1. Send OTP
        SendOtpRequest sendReq = SendOtpRequest.builder().phoneNumber(phone).build();
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP generated in test mode"))
                .andExpect(jsonPath("$.expiresInSeconds").exists());

        // 2. Retrieve OTP via debug endpoint
        mockMvc.perform(get("/api/auth/get-otp/" + phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(phone))
                .andExpect(jsonPath("$.otp").isString())
                .andExpect(jsonPath("$.otp").value(org.hamcrest.Matchers.matchesPattern("^[0-9]{6}$")));
    }

    @Test
    @DisplayName("POST /api/auth/resend-otp - Resend replacement OTP")
    void testResendOtp() throws Exception {
        String phone = "9110001113";

        // Initial send
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(phone))))
                .andExpect(status().isOk());

        // Clear cooldown to test resend endpoint logic
        otpService.clearOtpStorage();

        // Resend OTP
        ResendOtpRequest resendReq = ResendOtpRequest.builder().phoneNumber(phone).build();
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Replacement OTP generated in test mode"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp - With invalid OTP returns verified: false")
    void testVerifyOtp_Invalid() throws Exception {
        String phone = "9110001114";

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(phone))))
                .andExpect(status().isOk());

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .phoneNumber(phone)
                .otp("000000") // incorrect OTP
                .build();

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.isRegistered").value(false));
    }

    @Test
    @DisplayName("Complete Buyer Lifecycle: Verify OTP -> Signup -> Login -> Refresh -> Protected Access -> Logout")
    void testCompleteBuyerAuthLifecycle() throws Exception {
        String phone = "9110001115";

        // 1. Send OTP
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(phone))))
                .andExpect(status().isOk());

        // 2. Get the generated OTP
        MvcResult otpResult = mockMvc.perform(get("/api/auth/get-otp/" + phone))
                .andExpect(status().isOk())
                .andReturn();
        OtpDebugResponse otpDebug = objectMapper.readValue(otpResult.getResponse().getContentAsString(), OtpDebugResponse.class);
        String otp = otpDebug.getOtp();
        assertNotNull(otp);

        // 3. Verify OTP for unregistered user -> Expect verification token
        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .phoneNumber(phone)
                .otp(otp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.isRegistered").value(false))
                .andExpect(jsonPath("$.verificationToken").exists())
                .andReturn();

        VerifyOtpResponse verifyResp = objectMapper.readValue(verifyResult.getResponse().getContentAsString(), VerifyOtpResponse.class);
        String verificationToken = verifyResp.getVerificationToken();

        // 4. Signup using verification token
        SignUpRequest signUpReq = SignUpRequest.builder()
                .phoneNumber(phone)
                .verificationToken(verificationToken)
                .fullName("Anusha Rao")
                .email("anusha@gmail.com")
                .companyName("KFPCL Agro Export")
                .businessType("Exporter")
                .state("Andhra Pradesh")
                .city("Vijayawada")
                .fcmToken("fcm_device_test_token_1")
                .build();

        MvcResult signUpResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.fullName").value("Anusha Rao"))
                .andExpect(jsonPath("$.user.companyName").value("KFPCL Agro Export"))
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(signUpResult.getResponse().getContentAsString(), TokenResponse.class);
        String accessToken = tokenResp.getAccessToken();
        String refreshToken = tokenResp.getRefreshToken();
        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        // 5. Verify check-phone now returns exists = true
        mockMvc.perform(get("/api/auth/check-phone/" + phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        // 6. Access protected Customer Profile API with Bearer accessToken
        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Anusha Rao"))
                .andExpect(jsonPath("$.email").value("anusha@gmail.com"))
                .andExpect(jsonPath("$.companyName").value("KFPCL Agro Export"));

        // 7. Refresh access token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder().refreshToken(refreshToken).build();
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.user.fullName").value("Anusha Rao"))
                .andReturn();

        TokenResponse refreshedTokenResp = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        String newAccessToken = refreshedTokenResp.getAccessToken();
        assertNotNull(newAccessToken);

        // 8. Test Login with OTP for registered user
        otpService.clearOtpStorage();
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(phone))))
                .andExpect(status().isOk());

        MvcResult loginOtpResult = mockMvc.perform(get("/api/auth/get-otp/" + phone))
                .andExpect(status().isOk())
                .andReturn();
        String loginOtp = objectMapper.readValue(loginOtpResult.getResponse().getContentAsString(), OtpDebugResponse.class).getOtp();

        LoginRequest loginReq = LoginRequest.builder()
                .phoneNumber(phone)
                .otp(loginOtp)
                .fcmToken("fcm_device_test_token_2")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.fullName").value("Anusha Rao"));

        // 9. Logout
        LogoutRequest logoutReq = LogoutRequest.builder().refreshToken(refreshToken).build();
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("POST /api/auth/signup - Fails with invalid verification token")
    void testSignUp_InvalidVerificationToken() throws Exception {
        SignUpRequest req = SignUpRequest.builder()
                .phoneNumber("9110009999")
                .verificationToken("invalid")
                .fullName("Test User")
                .email("testuser@gmail.com")
                .companyName("Test Co")
                .businessType("Wholesaler")
                .state("Telangana")
                .city("Hyderabad")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Fails with invalid refresh token")
    void testRefresh_InvalidToken() throws Exception {
        RefreshTokenRequest req = RefreshTokenRequest.builder()
                .refreshToken("non_existent_refresh_token")
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Fails when user is not registered")
    void testLogin_UnregisteredUser() throws Exception {
        String unregisteredPhone = "9119998888";

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(unregisteredPhone))))
                .andExpect(status().isOk());

        MvcResult otpResult = mockMvc.perform(get("/api/auth/get-otp/" + unregisteredPhone))
                .andExpect(status().isOk())
                .andReturn();
        String otp = objectMapper.readValue(otpResult.getResponse().getContentAsString(), OtpDebugResponse.class).getOtp();

        LoginRequest loginReq = LoginRequest.builder()
                .phoneNumber(unregisteredPhone)
                .otp(otp)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not registered")));
    }

    // =========================================================================
    // 2. ADMIN PANEL AUTHENTICATION APIS (/api/auth/adminpanel/**)
    // =========================================================================

    @Test
    @DisplayName("POST /api/auth/adminpanel/login - Successful admin login")
    void testAdminLogin_Success() throws Exception {
        Map<String, String> req = Map.of("email", ADMIN_EMAIL, "password", "admin123");

        mockMvc.perform(post("/api/auth/adminpanel/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("POST /api/auth/adminpanel/login - Failed login with bad password")
    void testAdminLogin_Failure() throws Exception {
        Map<String, String> req = Map.of("email", ADMIN_EMAIL, "password", "wrongPassword");

        mockMvc.perform(post("/api/auth/adminpanel/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/adminpanel/refresh - Refresh admin session")
    void testAdminRefresh() throws Exception {
        mockMvc.perform(post("/api/auth/adminpanel/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Session refreshed"));
    }

    @Test
    @DisplayName("POST /api/auth/adminpanel/change-password - Change and reset admin password")
    void testAdminChangePasswordAndReset() throws Exception {
        // 1. Change password to new password
        Map<String, String> changeReq = Map.of(
                "email", ADMIN_EMAIL,
                "oldPassword", "admin123",
                "newPassword", "newAdmin456"
        );

        mockMvc.perform(post("/api/auth/adminpanel/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        // 2. Verify login with new password works
        mockMvc.perform(post("/api/auth/adminpanel/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", "newAdmin456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Reset password back using prepare-password-reset
        mockMvc.perform(post("/api/auth/adminpanel/prepare-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "newPassword", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Verify login with restored password works
        mockMvc.perform(post("/api/auth/adminpanel/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/adminpanel/logout - Admin logout")
    void testAdminLogout() throws Exception {
        mockMvc.perform(post("/api/auth/adminpanel/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // =========================================================================
    // 3. FIREBASE AUTHENTICATION & SECURITY TESTS (/api/auth/firebase-login)
    // =========================================================================

    @Test
    @DisplayName("POST /api/auth/firebase-login - Registered buyer login returns tokens and isRegistered: true")
    void testFirebaseLogin_RegisteredUser() throws Exception {
        String phone = "9876500001";
        // Seed buyer in buyer_users
        if (buyerUserRepository.findByPhoneNumber(phone).isEmpty()) {
            buyerUserRepository.save(com.project.kfpcl_exports.buyer.model.User.builder()
                    .phoneNumber(phone)
                    .fullName("Registered Buyer")
                    .email("buyer@gmail.com")
                    .companyName("KFPCL Buyer Co")
                    .businessType(com.project.kfpcl_exports.buyer.enums.BusinessType.WHOLESALER)
                    .state("Telangana")
                    .city("Hyderabad")
                    .enabled(true)
                    .status("ACTIVE")
                    .build());
        }

        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+91" + phone));
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("valid_registered_firebase_token")).thenReturn(mockToken);

        FirebaseLoginRequest request = FirebaseLoginRequest.builder()
                .idToken("valid_registered_firebase_token")
                .fcmToken("fcm_device_test_fb_1")
                .build();

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRegistered").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.fullName").value("Registered Buyer"))
                .andExpect(jsonPath("$.user.phoneNumber").value(phone));
    }

    @Test
    @DisplayName("POST /api/auth/firebase-login - Unregistered buyer returns isRegistered: false and verificationToken without creating user")
    void testFirebaseLogin_UnregisteredUser_GeneratesVerificationToken() throws Exception {
        String newPhone = "9876500002";

        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+91" + newPhone));
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("valid_unregistered_firebase_token")).thenReturn(mockToken);

        FirebaseLoginRequest request = FirebaseLoginRequest.builder()
                .idToken("valid_unregistered_firebase_token")
                .build();

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRegistered").value(false))
                .andExpect(jsonPath("$.verificationToken").exists())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        // Verify user was NOT created
        assertTrue(buyerUserRepository.findByPhoneNumber(newPhone).isEmpty(), "User must not be automatically created");
    }

    @Test
    @DisplayName("POST /api/auth/firebase-login -> Bridge to /api/auth/signup - Complete registration flow")
    void testFirebaseLogin_BridgeToSignup() throws Exception {
        String phone = "9876500003";

        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+91" + phone));
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("token_for_bridge_signup")).thenReturn(mockToken);

        // 1. Call firebase-login
        MvcResult fbResult = mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FirebaseLoginRequest("token_for_bridge_signup", null, null, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRegistered").value(false))
                .andExpect(jsonPath("$.verificationToken").exists())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(fbResult.getResponse().getContentAsString(), TokenResponse.class);
        String vToken = tokenResp.getVerificationToken();
        assertNotNull(vToken);

        // 2. Call signup using the verificationToken
        SignUpRequest signUpReq = SignUpRequest.builder()
                .phoneNumber(phone)
                .verificationToken(vToken)
                .fullName("Firebase New Buyer")
                .email("fbnewbuyer@gmail.com")
                .companyName("Firebase Exports Ltd")
                .businessType("Wholesaler")
                .state("Telangana")
                .city("Hyderabad")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.fullName").value("Firebase New Buyer"));

        // 3. Verify phone now exists in check-phone
        mockMvc.perform(get("/api/auth/check-phone/" + phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/signup - Replay prevention: verification token cannot be reused")
    void testFirebaseLogin_VerificationTokenReplayPrevented() throws Exception {
        String phone = "9876500004";

        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+91" + phone));
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("token_for_replay_test")).thenReturn(mockToken);

        MvcResult fbResult = mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FirebaseLoginRequest("token_for_replay_test", null, null, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(fbResult.getResponse().getContentAsString(), TokenResponse.class);
        String vToken = tokenResp.getVerificationToken();

        // First signup succeeds
        SignUpRequest req1 = SignUpRequest.builder()
                .phoneNumber(phone)
                .verificationToken(vToken)
                .fullName("User Once")
                .email("useronce@gmail.com")
                .companyName("Co")
                .businessType("Wholesaler")
                .state("Telangana")
                .city("Hyd")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Reusing the same verification token MUST fail
        SignUpRequest req2 = SignUpRequest.builder()
                .phoneNumber(phone)
                .verificationToken(vToken)
                .fullName("User Twice")
                .email("usertwice@gmail.com")
                .companyName("Co")
                .businessType("Wholesaler")
                .state("Telangana")
                .city("Hyd")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    @DisplayName("POST /api/auth/signup - Phone number mismatch rejection: token bound to another phone")
    void testFirebaseLogin_PhoneMismatchRejection() throws Exception {
        String boundPhone = "9876500005";
        String attackerPhone = "9876500099";

        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+91" + boundPhone));
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("token_bound_phone")).thenReturn(mockToken);

        MvcResult fbResult = mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FirebaseLoginRequest("token_bound_phone", null, null, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(fbResult.getResponse().getContentAsString(), TokenResponse.class);
        String vToken = tokenResp.getVerificationToken();

        // Attempting to signup attackerPhone using boundPhone's token
        SignUpRequest mismatchReq = SignUpRequest.builder()
                .phoneNumber(attackerPhone)
                .verificationToken(vToken)
                .fullName("Attacker")
                .email("attacker@gmail.com")
                .companyName("Co")
                .businessType("Wholesaler")
                .state("Telangana")
                .city("Hyd")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    @DisplayName("POST /api/auth/firebase-login - Rejects invalid or expired token with 400 Bad Request")
    void testFirebaseLogin_InvalidToken() throws Exception {
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("invalid_fb_token"))
                .thenThrow(new IllegalArgumentException("Invalid Firebase ID token: Token is expired"));

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FirebaseLoginRequest("invalid_fb_token", null, null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid Firebase ID token")));
    }

    @Test
    @DisplayName("POST /api/auth/firebase-login - Rejects token missing phone_number claim")
    void testFirebaseLogin_MissingPhoneClaim() throws Exception {
        com.google.firebase.auth.FirebaseToken mockToken = org.mockito.Mockito.mock(com.google.firebase.auth.FirebaseToken.class);
        org.mockito.Mockito.when(mockToken.getClaims()).thenReturn(Map.of()); // No phone_number claim
        org.mockito.Mockito.when(firebaseAuthService.verifyIdToken("token_missing_phone")).thenReturn(mockToken);

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FirebaseLoginRequest("token_missing_phone", null, null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Firebase ID token must contain a verified phone number claim"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp - Brute-force lockout: 5 failed attempts invalidates OTP")
    void testOtp_BruteForceLockout() throws Exception {
        String phone = "9110009977";

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendOtpRequest(phone))))
                .andExpect(status().isOk());

        // Get actual OTP
        MvcResult otpRes = mockMvc.perform(get("/api/auth/get-otp/" + phone)).andReturn();
        String actualOtp = objectMapper.readValue(otpRes.getResponse().getContentAsString(), OtpDebugResponse.class).getOtp();

        // 4 failed attempts
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyOtpRequest(phone, "000000", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(false));
        }

        // 5th failed attempt triggers purge
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(phone, "000000", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));

        // Even with the actual valid OTP, verification must now fail because OTP was purged
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest(phone, actualOtp, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }
}
