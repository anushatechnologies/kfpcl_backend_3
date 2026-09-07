package com.project.kfpcl_exports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.dto.AuthDTOs.*;
import com.project.kfpcl_exports.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Developer1ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @Test
    void testCompleteDeveloper1Flow() throws Exception {
        String phone = "9876543210";

        // 1. Check phone prior to registration
        mockMvc.perform(get("/api/auth/check-phone/" + phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        // 2. Send OTP
        SendOtpRequest sendOtpReq = SendOtpRequest.builder().phoneNumber(phone).build();
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Fetch the generated OTP from active OTP storage
        OtpService.OtpData activeOtpData = otpService.getActiveOtpData(phone);
        assertNotNull(activeOtpData, "OTP data should exist after send-otp call");
        String activeOtp = activeOtpData.getOtp();

        // 3. Verify OTP
        VerifyOtpRequest verifyOtpReq = VerifyOtpRequest.builder().phoneNumber(phone).otp(activeOtp).build();
        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.isRegistered").value(false))
                .andReturn();

        VerifyOtpResponse verifyResp = objectMapper.readValue(
                verifyResult.getResponse().getContentAsString(), VerifyOtpResponse.class
        );
        assertNotNull(verifyResp.getVerificationToken());

        // 4. Signup buyer account
        SignUpRequest signUpReq = SignUpRequest.builder()
                .phoneNumber(phone)
                .verificationToken(verifyResp.getVerificationToken())
                .fullName("Rahul Sharma")
                .email("rahul@kfpcl.com")
                .companyName("KFPCL Traders")
                .businessType("Wholesaler")
                .state("Maharashtra")
                .city("Pune")
                .fcmToken("sample_fcm_token_123")
                .build();

        MvcResult signUpResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        TokenResponse tokenResp = objectMapper.readValue(
                signUpResult.getResponse().getContentAsString(), TokenResponse.class
        );
        String accessToken = tokenResp.getAccessToken();

        // 5. Check phone post-registration
        mockMvc.perform(get("/api/auth/check-phone/" + phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        // 6. Get Profile with Bearer token
        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.companyName").value("KFPCL Traders"));

        // 7. Create Warehouse Address
        AddressRequest addressReq = AddressRequest.builder()
                .addressType("Warehouse")
                .houseNo("Gate No. 4")
                .streetDetails("APMC Market Yard, Market Yard Road")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411037")
                .isDefault(true)
                .build();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        // 8. Get Addresses
        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Pune"));

        // 9. Save FCM Token
        FcmTokenRequest fcmReq = FcmTokenRequest.builder()
                .fcmToken("updated_fcm_token_456")
                .deviceType("ANDROID")
                .build();

        mockMvc.perform(post("/api/save-token")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fcmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 10. Check Policies & Version
        mockMvc.perform(get("/api/policies/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("privacy-policy"));

        mockMvc.perform(get("/api/app/version?platform=android"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minVersion").value("1.0.0"));
    }
}
