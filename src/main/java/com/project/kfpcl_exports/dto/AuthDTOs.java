package com.project.kfpcl_exports.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AuthDTOs {

    public static class OtpDebugResponse {
        private String phoneNumber;
        private String otp;

        public OtpDebugResponse() {}
        public OtpDebugResponse(String phoneNumber, String otp) {
            this.phoneNumber = phoneNumber;
            this.otp = otp;
        }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        public static OtpDebugResponseBuilder builder() { return new OtpDebugResponseBuilder(); }
        public static class OtpDebugResponseBuilder {
            private String phoneNumber, otp;
            public OtpDebugResponseBuilder phoneNumber(String v) { phoneNumber = v; return this; }
            public OtpDebugResponseBuilder otp(String v) { otp = v; return this; }
            public OtpDebugResponse build() { return new OtpDebugResponse(phoneNumber, otp); }
        }
    }

    public static class CheckPhoneResponse {
        @JsonProperty("exists")
        private boolean exists;

        public CheckPhoneResponse() {}
        public CheckPhoneResponse(boolean exists) { this.exists = exists; }
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }

        public static CheckPhoneResponseBuilder builder() { return new CheckPhoneResponseBuilder(); }
        public static class CheckPhoneResponseBuilder {
            private boolean exists;
            public CheckPhoneResponseBuilder exists(boolean exists) { this.exists = exists; return this; }
            public CheckPhoneResponse build() { return new CheckPhoneResponse(exists); }
        }
    }

    public static class SendOtpRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be valid 10 to 15 digits")
        private String phoneNumber;

        public SendOtpRequest() {}
        public SendOtpRequest(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public static SendOtpRequestBuilder builder() { return new SendOtpRequestBuilder(); }
        public static class SendOtpRequestBuilder {
            private String phoneNumber;
            public SendOtpRequestBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
            public SendOtpRequest build() { return new SendOtpRequest(phoneNumber); }
        }
    }

    public static class SendOtpResponse {
        @JsonProperty("success")
        private boolean success;
        private String message;
        private long expiresInSeconds;

        public SendOtpResponse() {}
        public SendOtpResponse(boolean success, String message, long expiresInSeconds) {
            this.success = success;
            this.message = message;
            this.expiresInSeconds = expiresInSeconds;
        }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

        public static SendOtpResponseBuilder builder() { return new SendOtpResponseBuilder(); }
        public static class SendOtpResponseBuilder {
            private boolean success;
            private String message;
            private long expiresInSeconds;
            public SendOtpResponseBuilder success(boolean success) { this.success = success; return this; }
            public SendOtpResponseBuilder message(String message) { this.message = message; return this; }
            public SendOtpResponseBuilder expiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; return this; }
            public SendOtpResponse build() { return new SendOtpResponse(success, message, expiresInSeconds); }
        }
    }

    public static class VerifyOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otp;

        public VerifyOtpRequest() {}
        public VerifyOtpRequest(String phoneNumber, String otp) {
            this.phoneNumber = phoneNumber;
            this.otp = otp;
        }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        public static VerifyOtpRequestBuilder builder() { return new VerifyOtpRequestBuilder(); }
        public static class VerifyOtpRequestBuilder {
            private String phoneNumber;
            private String otp;
            public VerifyOtpRequestBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
            public VerifyOtpRequestBuilder otp(String otp) { this.otp = otp; return this; }
            public VerifyOtpRequest build() { return new VerifyOtpRequest(phoneNumber, otp); }
        }
    }

    public static class VerifyOtpResponse {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("verified")
        private boolean verified;

        @JsonProperty("isRegistered")
        private boolean isRegistered;

        private String verificationToken;
        private String accessToken;
        private String refreshToken;
        private UserProfileResponse user;

        public VerifyOtpResponse() {}
        public VerifyOtpResponse(boolean success, boolean verified, boolean isRegistered, String verificationToken, String accessToken, String refreshToken, UserProfileResponse user) {
            this.success = success;
            this.verified = verified;
            this.isRegistered = isRegistered;
            this.verificationToken = verificationToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

        public boolean isRegistered() { return isRegistered; }
        public void setRegistered(boolean registered) { isRegistered = registered; }

        public String getVerificationToken() { return verificationToken; }
        public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public UserProfileResponse getUser() { return user; }
        public void setUser(UserProfileResponse user) { this.user = user; }

        public static VerifyOtpResponseBuilder builder() { return new VerifyOtpResponseBuilder(); }
        public static class VerifyOtpResponseBuilder {
            private boolean success;
            private boolean verified;
            private boolean isRegistered;
            private String verificationToken;
            private String accessToken;
            private String refreshToken;
            private UserProfileResponse user;

            public VerifyOtpResponseBuilder success(boolean success) { this.success = success; return this; }
            public VerifyOtpResponseBuilder verified(boolean verified) { this.verified = verified; return this; }
            public VerifyOtpResponseBuilder isRegistered(boolean isRegistered) { this.isRegistered = isRegistered; return this; }
            public VerifyOtpResponseBuilder verificationToken(String verificationToken) { this.verificationToken = verificationToken; return this; }
            public VerifyOtpResponseBuilder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
            public VerifyOtpResponseBuilder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
            public VerifyOtpResponseBuilder user(UserProfileResponse user) { this.user = user; return this; }
            public VerifyOtpResponse build() { return new VerifyOtpResponse(success, verified, isRegistered, verificationToken, accessToken, refreshToken, user); }
        }
    }

    public static class ResendOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        public ResendOtpRequest() {}
        public ResendOtpRequest(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public static ResendOtpRequestBuilder builder() { return new ResendOtpRequestBuilder(); }
        public static class ResendOtpRequestBuilder {
            private String phoneNumber;
            public ResendOtpRequestBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
            public ResendOtpRequest build() { return new ResendOtpRequest(phoneNumber); }
        }
    }

    public static class SignUpRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "Verification token is required")
        private String verificationToken;

        @NotBlank(message = "Full name is required")
        @Size(min = 3, message = "Full name must be at least 3 characters")
        private String fullName;

        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Company name is required")
        private String companyName;

        @NotBlank(message = "Business type is required")
        private String businessType;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "City is required")
        private String city;

        private String fcmToken;

        public SignUpRequest() {}
        public SignUpRequest(String phoneNumber, String verificationToken, String fullName, String email, String companyName, String businessType, String state, String city, String fcmToken) {
            this.phoneNumber = phoneNumber;
            this.verificationToken = verificationToken;
            this.fullName = fullName;
            this.email = email;
            this.companyName = companyName;
            this.businessType = businessType;
            this.state = state;
            this.city = city;
            this.fcmToken = fcmToken;
        }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getVerificationToken() { return verificationToken; }
        public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

        public static SignUpRequestBuilder builder() { return new SignUpRequestBuilder(); }
        public static class SignUpRequestBuilder {
            private String phoneNumber, verificationToken, fullName, email, companyName, businessType, state, city, fcmToken;
            public SignUpRequestBuilder phoneNumber(String v) { phoneNumber = v; return this; }
            public SignUpRequestBuilder verificationToken(String v) { verificationToken = v; return this; }
            public SignUpRequestBuilder fullName(String v) { fullName = v; return this; }
            public SignUpRequestBuilder email(String v) { email = v; return this; }
            public SignUpRequestBuilder companyName(String v) { companyName = v; return this; }
            public SignUpRequestBuilder businessType(String v) { businessType = v; return this; }
            public SignUpRequestBuilder state(String v) { state = v; return this; }
            public SignUpRequestBuilder city(String v) { city = v; return this; }
            public SignUpRequestBuilder fcmToken(String v) { fcmToken = v; return this; }
            public SignUpRequest build() { return new SignUpRequest(phoneNumber, verificationToken, fullName, email, companyName, businessType, state, city, fcmToken); }
        }
    }

    public static class LoginRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "OTP is required")
        private String otp;

        private String fcmToken;

        public LoginRequest() {}
        public LoginRequest(String phoneNumber, String otp, String fcmToken) {
            this.phoneNumber = phoneNumber;
            this.otp = otp;
            this.fcmToken = fcmToken;
        }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

        public static LoginRequestBuilder builder() { return new LoginRequestBuilder(); }
        public static class LoginRequestBuilder {
            private String phoneNumber, otp, fcmToken;
            public LoginRequestBuilder phoneNumber(String v) { phoneNumber = v; return this; }
            public LoginRequestBuilder otp(String v) { otp = v; return this; }
            public LoginRequestBuilder fcmToken(String v) { fcmToken = v; return this; }
            public LoginRequest build() { return new LoginRequest(phoneNumber, otp, fcmToken); }
        }
    }

    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        public RefreshTokenRequest() {}
        public RefreshTokenRequest(String refreshToken) { this.refreshToken = refreshToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public static RefreshTokenRequestBuilder builder() { return new RefreshTokenRequestBuilder(); }
        public static class RefreshTokenRequestBuilder {
            private String refreshToken;
            public RefreshTokenRequestBuilder refreshToken(String v) { refreshToken = v; return this; }
            public RefreshTokenRequest build() { return new RefreshTokenRequest(refreshToken); }
        }
    }

    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private UserProfileResponse user;

        public TokenResponse() {}
        public TokenResponse(String accessToken, String refreshToken, UserProfileResponse user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public UserProfileResponse getUser() { return user; }
        public void setUser(UserProfileResponse user) { this.user = user; }

        public static TokenResponseBuilder builder() { return new TokenResponseBuilder(); }
        public static class TokenResponseBuilder {
            private String accessToken, refreshToken;
            private UserProfileResponse user;
            public TokenResponseBuilder accessToken(String v) { accessToken = v; return this; }
            public TokenResponseBuilder refreshToken(String v) { refreshToken = v; return this; }
            public TokenResponseBuilder user(UserProfileResponse v) { user = v; return this; }
            public TokenResponse build() { return new TokenResponse(accessToken, refreshToken, user); }
        }
    }

    public static class LogoutRequest {
        private String refreshToken;

        public LogoutRequest() {}
        public LogoutRequest(String refreshToken) { this.refreshToken = refreshToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public static LogoutRequestBuilder builder() { return new LogoutRequestBuilder(); }
        public static class LogoutRequestBuilder {
            private String refreshToken;
            public LogoutRequestBuilder refreshToken(String v) { refreshToken = v; return this; }
            public LogoutRequest build() { return new LogoutRequest(refreshToken); }
        }
    }

    public static class ProfileUpdateRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 3, message = "Full name must be at least 3 characters")
        private String fullName;

        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Company name is required")
        private String companyName;

        @NotBlank(message = "Business type is required")
        private String businessType;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "City is required")
        private String city;

        public ProfileUpdateRequest() {}
        public ProfileUpdateRequest(String fullName, String email, String companyName, String businessType, String state, String city) {
            this.fullName = fullName;
            this.email = email;
            this.companyName = companyName;
            this.businessType = businessType;
            this.state = state;
            this.city = city;
        }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public static ProfileUpdateRequestBuilder builder() { return new ProfileUpdateRequestBuilder(); }
        public static class ProfileUpdateRequestBuilder {
            private String fullName, email, companyName, businessType, state, city;
            public ProfileUpdateRequestBuilder fullName(String v) { fullName = v; return this; }
            public ProfileUpdateRequestBuilder email(String v) { email = v; return this; }
            public ProfileUpdateRequestBuilder companyName(String v) { companyName = v; return this; }
            public ProfileUpdateRequestBuilder businessType(String v) { businessType = v; return this; }
            public ProfileUpdateRequestBuilder state(String v) { state = v; return this; }
            public ProfileUpdateRequestBuilder city(String v) { city = v; return this; }
            public ProfileUpdateRequest build() { return new ProfileUpdateRequest(fullName, email, companyName, businessType, state, city); }
        }
    }

    public static class UserProfileResponse {
        private Long id;
        private String phoneNumber;
        private String fullName;
        private String email;
        private String companyName;
        private String businessType;
        private String state;
        private String city;

        @JsonProperty("isVerified")
        private Boolean isVerified;

        @JsonProperty("isActive")
        private Boolean isActive;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserProfileResponse() {}
        public UserProfileResponse(Long id, String phoneNumber, String fullName, String email, String companyName, String businessType, String state, String city, Boolean isVerified, Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.phoneNumber = phoneNumber;
            this.fullName = fullName;
            this.email = email;
            this.companyName = companyName;
            this.businessType = businessType;
            this.state = state;
            this.city = city;
            this.isVerified = isVerified;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public static UserProfileResponseBuilder builder() { return new UserProfileResponseBuilder(); }
        public static class UserProfileResponseBuilder {
            private Long id;
            private String phoneNumber, fullName, email, companyName, businessType, state, city;
            private Boolean isVerified, isActive;
            private LocalDateTime createdAt, updatedAt;
            public UserProfileResponseBuilder id(Long v) { id = v; return this; }
            public UserProfileResponseBuilder phoneNumber(String v) { phoneNumber = v; return this; }
            public UserProfileResponseBuilder fullName(String v) { fullName = v; return this; }
            public UserProfileResponseBuilder email(String v) { email = v; return this; }
            public UserProfileResponseBuilder companyName(String v) { companyName = v; return this; }
            public UserProfileResponseBuilder businessType(String v) { businessType = v; return this; }
            public UserProfileResponseBuilder state(String v) { state = v; return this; }
            public UserProfileResponseBuilder city(String v) { city = v; return this; }
            public UserProfileResponseBuilder isVerified(Boolean v) { isVerified = v; return this; }
            public UserProfileResponseBuilder isActive(Boolean v) { isActive = v; return this; }
            public UserProfileResponseBuilder createdAt(LocalDateTime v) { createdAt = v; return this; }
            public UserProfileResponseBuilder updatedAt(LocalDateTime v) { updatedAt = v; return this; }
            public UserProfileResponse build() { return new UserProfileResponse(id, phoneNumber, fullName, email, companyName, businessType, state, city, isVerified, isActive, createdAt, updatedAt); }
        }
    }

    public static class AddressRequest {
        private String addressType;
        private String houseNo;

        @NotBlank(message = "Street details are required")
        private String streetDetails;

        private String landmark;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "Pincode is required")
        private String pincode;

        @JsonProperty("isDefault")
        private Boolean isDefault;

        public AddressRequest() {}
        public AddressRequest(String addressType, String houseNo, String streetDetails, String landmark, String city, String state, String pincode, Boolean isDefault) {
            this.addressType = addressType;
            this.houseNo = houseNo;
            this.streetDetails = streetDetails;
            this.landmark = landmark;
            this.city = city;
            this.state = state;
            this.pincode = pincode;
            this.isDefault = isDefault;
        }

        public String getAddressType() { return addressType; }
        public void setAddressType(String addressType) { this.addressType = addressType; }
        public String getHouseNo() { return houseNo; }
        public void setHouseNo(String houseNo) { this.houseNo = houseNo; }
        public String getStreetDetails() { return streetDetails; }
        public void setStreetDetails(String streetDetails) { this.streetDetails = streetDetails; }
        public String getLandmark() { return landmark; }
        public void setLandmark(String landmark) { this.landmark = landmark; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

        public static AddressRequestBuilder builder() { return new AddressRequestBuilder(); }
        public static class AddressRequestBuilder {
            private String addressType, houseNo, streetDetails, landmark, city, state, pincode;
            private Boolean isDefault;
            public AddressRequestBuilder addressType(String v) { addressType = v; return this; }
            public AddressRequestBuilder houseNo(String v) { houseNo = v; return this; }
            public AddressRequestBuilder streetDetails(String v) { streetDetails = v; return this; }
            public AddressRequestBuilder landmark(String v) { landmark = v; return this; }
            public AddressRequestBuilder city(String v) { city = v; return this; }
            public AddressRequestBuilder state(String v) { state = v; return this; }
            public AddressRequestBuilder pincode(String v) { pincode = v; return this; }
            public AddressRequestBuilder isDefault(Boolean v) { isDefault = v; return this; }
            public AddressRequest build() { return new AddressRequest(addressType, houseNo, streetDetails, landmark, city, state, pincode, isDefault); }
        }
    }

    public static class AddressResponse {
        private Long id;
        private Long userId;
        private String addressType;
        private String houseNo;
        private String streetDetails;
        private String landmark;
        private String city;
        private String state;
        private String pincode;

        @JsonProperty("isDefault")
        private Boolean isDefault;

        private LocalDateTime createdAt;

        public AddressResponse() {}
        public AddressResponse(Long id, Long userId, String addressType, String houseNo, String streetDetails, String landmark, String city, String state, String pincode, Boolean isDefault, LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.addressType = addressType;
            this.houseNo = houseNo;
            this.streetDetails = streetDetails;
            this.landmark = landmark;
            this.city = city;
            this.state = state;
            this.pincode = pincode;
            this.isDefault = isDefault;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAddressType() { return addressType; }
        public void setAddressType(String addressType) { this.addressType = addressType; }
        public String getHouseNo() { return houseNo; }
        public void setHouseNo(String houseNo) { this.houseNo = houseNo; }
        public String getStreetDetails() { return streetDetails; }
        public void setStreetDetails(String streetDetails) { this.streetDetails = streetDetails; }
        public String getLandmark() { return landmark; }
        public void setLandmark(String landmark) { this.landmark = landmark; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public static AddressResponseBuilder builder() { return new AddressResponseBuilder(); }
        public static class AddressResponseBuilder {
            private Long id, userId;
            private String addressType, houseNo, streetDetails, landmark, city, state, pincode;
            private Boolean isDefault;
            private LocalDateTime createdAt;
            public AddressResponseBuilder id(Long v) { id = v; return this; }
            public AddressResponseBuilder userId(Long v) { userId = v; return this; }
            public AddressResponseBuilder addressType(String v) { addressType = v; return this; }
            public AddressResponseBuilder houseNo(String v) { houseNo = v; return this; }
            public AddressResponseBuilder streetDetails(String v) { streetDetails = v; return this; }
            public AddressResponseBuilder landmark(String v) { landmark = v; return this; }
            public AddressResponseBuilder city(String v) { city = v; return this; }
            public AddressResponseBuilder state(String v) { state = v; return this; }
            public AddressResponseBuilder pincode(String v) { pincode = v; return this; }
            public AddressResponseBuilder isDefault(Boolean v) { isDefault = v; return this; }
            public AddressResponseBuilder createdAt(LocalDateTime v) { createdAt = v; return this; }
            public AddressResponse build() { return new AddressResponse(id, userId, addressType, houseNo, streetDetails, landmark, city, state, pincode, isDefault, createdAt); }
        }
    }

    public static class FcmTokenRequest {
        @NotBlank(message = "FCM token is required")
        private String fcmToken;
        private String deviceType;

        public FcmTokenRequest() {}
        public FcmTokenRequest(String fcmToken, String deviceType) {
            this.fcmToken = fcmToken;
            this.deviceType = deviceType;
        }

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public static FcmTokenRequestBuilder builder() { return new FcmTokenRequestBuilder(); }
        public static class FcmTokenRequestBuilder {
            private String fcmToken, deviceType;
            public FcmTokenRequestBuilder fcmToken(String v) { fcmToken = v; return this; }
            public FcmTokenRequestBuilder deviceType(String v) { deviceType = v; return this; }
            public FcmTokenRequest build() { return new FcmTokenRequest(fcmToken, deviceType); }
        }
    }

    public static class PolicyResponse {
        private String type;
        private String content;

        public PolicyResponse() {}
        public PolicyResponse(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public static PolicyResponseBuilder builder() { return new PolicyResponseBuilder(); }
        public static class PolicyResponseBuilder {
            private String type, content;
            public PolicyResponseBuilder type(String v) { type = v; return this; }
            public PolicyResponseBuilder content(String v) { content = v; return this; }
            public PolicyResponse build() { return new PolicyResponse(type, content); }
        }
    }

    public static class AppVersionResponse {
        private String minVersion;
        private String latestVersion;
        private boolean forceUpdate;

        public AppVersionResponse() {}
        public AppVersionResponse(String minVersion, String latestVersion, boolean forceUpdate) {
            this.minVersion = minVersion;
            this.latestVersion = latestVersion;
            this.forceUpdate = forceUpdate;
        }

        public String getMinVersion() { return minVersion; }
        public void setMinVersion(String minVersion) { this.minVersion = minVersion; }
        public String getLatestVersion() { return latestVersion; }
        public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
        public boolean isForceUpdate() { return forceUpdate; }
        public void setForceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; }

        public static AppVersionResponseBuilder builder() { return new AppVersionResponseBuilder(); }
        public static class AppVersionResponseBuilder {
            private String minVersion, latestVersion;
            private boolean forceUpdate;
            public AppVersionResponseBuilder minVersion(String v) { minVersion = v; return this; }
            public AppVersionResponseBuilder latestVersion(String v) { latestVersion = v; return this; }
            public AppVersionResponseBuilder forceUpdate(boolean v) { forceUpdate = v; return this; }
            public AppVersionResponse build() { return new AppVersionResponse(minVersion, latestVersion, forceUpdate); }
        }
    }

    public static class GenericResponse {
        @JsonProperty("success")
        private boolean success;
        private String message;

        public GenericResponse() {}
        public GenericResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public static GenericResponseBuilder builder() { return new GenericResponseBuilder(); }
        public static class GenericResponseBuilder {
            private boolean success;
            private String message;
            public GenericResponseBuilder success(boolean v) { success = v; return this; }
            public GenericResponseBuilder message(String v) { message = v; return this; }
            public GenericResponse build() { return new GenericResponse(success, message); }
        }
    }
}
