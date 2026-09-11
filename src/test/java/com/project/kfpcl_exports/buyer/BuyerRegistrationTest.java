package com.project.kfpcl_exports.buyer;

import com.project.kfpcl_exports.buyer.dto.RegistrationRequestDto;
import com.project.kfpcl_exports.buyer.dto.RegistrationResponseDto;
import com.project.kfpcl_exports.buyer.enums.BusinessType;
import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import com.project.kfpcl_exports.buyer.service.BuyerRegistrationService;
import com.project.kfpcl_exports.config.AwsS3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerRegistrationTest {

    @Mock
    private UserRepository buyerUserRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private AwsS3Config awsS3Config;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.project.kfpcl_exports.service.TokenService tokenService;

    @InjectMocks
    private BuyerRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "storageType", "local");
        ReflectionTestUtils.setField(registrationService, "localUploadDir", "./target/test_uploads");
        lenient().when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        lenient().when(tokenService.createAccessToken(any(String.class), any())).thenReturn("mock_jwt_access_token");
        lenient().when(tokenService.createRefreshToken(any(String.class), any())).thenReturn("mock_jwt_refresh_token");
    }

    @Test
    @DisplayName("Should successfully register buyer with valid PAN and GSTIN files")
    void testSuccessfulRegistration() {
        MockMultipartFile panFile = new MockMultipartFile(
                "panCardImage", "pan.png", "image/png", new byte[1024]
        );
        MockMultipartFile gstinFile = new MockMultipartFile(
                "gstinPhoto", "gstin.pdf", "application/pdf", new byte[2048]
        );

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setFullName("Sai Krishna");
        dto.setMobileNumber("9274370580");
        dto.setEmail("saikrishna@gmail.com");
        dto.setCompanyName("KFPCL Exports");
        dto.setBusinessType(BusinessType.WHOLESALER);
        dto.setState("Telangana");
        dto.setCity("Hyderabad");
        dto.setGstin("36AAAAA0000A1Z5");
        dto.setGstinPhoto(gstinFile);
        dto.setPanNumber("ABCDE1234F");
        dto.setPanCardImage(panFile);

        when(buyerUserRepository.existsByEmailIgnoreCase(dto.getEmail())).thenReturn(false);
        when(buyerUserRepository.existsByPhoneNumber(dto.getMobileNumber())).thenReturn(false);
        when(buyerUserRepository.existsByPanNumberIgnoreCase(dto.getPanNumber())).thenReturn(false);

        when(buyerUserRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("test-uuid-1234");
            return user;
        });

        RegistrationResponseDto response = registrationService.registerBuyer(dto);

        assertNotNull(response);
        assertEquals("test-uuid-1234", response.getId());
        assertEquals("Sai Krishna", response.getFullName());
        assertEquals("saikrishna@gmail.com", response.getEmail());
        assertEquals("9274370580", response.getPhoneNumber());
        assertEquals("ABCDE1234F", response.getPanNumber());
        assertNotNull(response.getPanCardUrl());
        assertNotNull(response.getGstinPhotoUrl());
        verify(buyerUserRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when PAN card file exceeds 1 MB limit")
    void testPanFileTooLarge() {
        // 1.5 MB file (greater than 1,048,576 bytes)
        byte[] largeBytes = new byte[1_500_000];
        MockMultipartFile largePan = new MockMultipartFile(
                "panCardImage", "pan.jpg", "image/jpeg", largeBytes
        );

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setFullName("Test User");
        dto.setMobileNumber("9876543210");
        dto.setEmail("test@gmail.com");
        dto.setCompanyName("Test Co");
        dto.setBusinessType(BusinessType.TRADER);
        dto.setState("AP");
        dto.setCity("Vijayawada");
        dto.setPanNumber("ABCDE1234F");
        dto.setPanCardImage(largePan);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerBuyer(dto)
        );

        assertTrue(ex.getMessage().contains("exceeds the strict 1 MB limit"));
    }

    @Test
    @DisplayName("Should fail when GSTIN photo exceeds 500 KB limit")
    void testGstinPhotoTooLarge() {
        MockMultipartFile panFile = new MockMultipartFile(
                "panCardImage", "pan.png", "image/png", new byte[1024]
        );
        // 600 KB file (greater than 512,000 bytes)
        MockMultipartFile largeGstin = new MockMultipartFile(
                "gstinPhoto", "gst.pdf", "application/pdf", new byte[600_000]
        );

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setFullName("Test User");
        dto.setMobileNumber("9876543210");
        dto.setEmail("test@gmail.com");
        dto.setCompanyName("Test Co");
        dto.setBusinessType(BusinessType.RETAILER);
        dto.setState("AP");
        dto.setCity("Guntur");
        dto.setPanNumber("ABCDE1234F");
        dto.setPanCardImage(panFile);
        dto.setGstin("37AAAAA0000A1Z5");
        dto.setGstinPhoto(largeGstin);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerBuyer(dto)
        );

        assertTrue(ex.getMessage().contains("exceeds the strict 500 KB limit"));
    }

    @Test
    @DisplayName("Should fail when duplicate PAN is detected")
    void testDuplicatePan() {
        MockMultipartFile panFile = new MockMultipartFile(
                "panCardImage", "pan.png", "image/png", new byte[1024]
        );

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setFullName("Test User");
        dto.setMobileNumber("9876543210");
        dto.setEmail("test@gmail.com");
        dto.setCompanyName("Test Co");
        dto.setBusinessType(BusinessType.WHOLESALER);
        dto.setState("AP");
        dto.setCity("Vizag");
        dto.setPanNumber("ABCDE1234F");
        dto.setPanCardImage(panFile);

        when(buyerUserRepository.existsByPanNumberIgnoreCase("ABCDE1234F")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerBuyer(dto)
        );

        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Should successfully login buyer and issue JWT access and refresh tokens")
    void testSuccessfulLogin() {
        User user = User.builder()
                .id("buyer-uuid-999")
                .fullName("Sai Krishna")
                .email("saikrishna@gmail.com")
                .phoneNumber("9274370580")
                .password("encoded_secret")
                .enabled(true)
                .build();

        when(buyerUserRepository.findByPhoneNumber("9274370580")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("myPassword123", "encoded_secret")).thenReturn(true);

        com.project.kfpcl_exports.buyer.dto.BuyerLoginRequest request = new com.project.kfpcl_exports.buyer.dto.BuyerLoginRequest(
                "9274370580", "myPassword123"
        );

        com.project.kfpcl_exports.buyer.dto.BuyerLoginResponse response = registrationService.loginBuyer(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("mock_jwt_access_token", response.getAccessToken());
        assertEquals("mock_jwt_refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("buyer-uuid-999", response.getBuyer().getId());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void testLoginInvalidPassword() {
        User user = User.builder()
                .id("buyer-uuid-999")
                .fullName("Sai Krishna")
                .email("saikrishna@gmail.com")
                .phoneNumber("9274370580")
                .password("encoded_secret")
                .enabled(true)
                .build();

        when(buyerUserRepository.findByPhoneNumber("9274370580")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encoded_secret")).thenReturn(false);

        com.project.kfpcl_exports.buyer.dto.BuyerLoginRequest request = new com.project.kfpcl_exports.buyer.dto.BuyerLoginRequest(
                "9274370580", "wrongPassword"
        );

        assertThrows(IllegalArgumentException.class, () -> registrationService.loginBuyer(request));
    }

    @Test
    @DisplayName("Should validate email and reject domains other than @gmail.com (such as @message or @hi)")
    void testEmailValidationOnlyAllowsGmail() {
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        RegistrationRequestDto invalidDto1 = new RegistrationRequestDto();
        invalidDto1.setEmail("user@message");
        assertFalse(validator.validateProperty(invalidDto1, "email").isEmpty());

        RegistrationRequestDto invalidDto2 = new RegistrationRequestDto();
        invalidDto2.setEmail("user@hi");
        assertFalse(validator.validateProperty(invalidDto2, "email").isEmpty());

        RegistrationRequestDto invalidDto3 = new RegistrationRequestDto();
        invalidDto3.setEmail("user@yahoo.com");
        assertFalse(validator.validateProperty(invalidDto3, "email").isEmpty());

        RegistrationRequestDto validDto = new RegistrationRequestDto();
        validDto.setEmail("user@gmail.com");
        assertTrue(validator.validateProperty(validDto, "email").isEmpty());
    }
}
