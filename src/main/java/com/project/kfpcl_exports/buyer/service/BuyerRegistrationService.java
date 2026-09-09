package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.config.AwsS3Config;
import com.project.kfpcl_exports.buyer.dto.RegistrationRequestDto;
import com.project.kfpcl_exports.buyer.dto.RegistrationResponseDto;
import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyerRegistrationService {

    private static final long MAX_PAN_SIZE = 1_048_576L;   // 1 MB strict
    private static final long MAX_GSTIN_SIZE = 512_000L;    // 500 KB strict
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private final UserRepository buyerUserRepository;
    private final S3Client s3Client;
    private final AwsS3Config awsS3Config;
    private final PasswordEncoder passwordEncoder;
    private final com.project.kfpcl_exports.service.TokenService tokenService;

    @Value("${buyer.storage.type:s3}")
    private String storageType;

    @Value("${buyer.storage.local-dir:./uploads/buyer_documents}")
    private String localUploadDir;

    @Value("${buyer.storage.s3-folder:buyer_documents}")
    private String s3Folder;

    @Transactional
    public RegistrationResponseDto registerBuyer(RegistrationRequestDto dto) {
        log.info("Processing buyer registration for email: {}, phone: {}", dto.getEmail(), dto.getMobileNumber());

        // 1. Uniqueness Checks
        if (buyerUserRepository.existsByEmailIgnoreCase(dto.getEmail().trim())) {
            throw new IllegalArgumentException("A buyer with email '" + dto.getEmail() + "' is already registered");
        }
        if (buyerUserRepository.existsByPhoneNumber(dto.getMobileNumber().trim())) {
            throw new IllegalArgumentException("A buyer with phone number '" + dto.getMobileNumber() + "' is already registered");
        }
        if (buyerUserRepository.existsByPanNumberIgnoreCase(dto.getPanNumber().trim())) {
            throw new IllegalArgumentException("A buyer with PAN number '" + dto.getPanNumber() + "' is already registered");
        }

        // 2. Validate PAN File (Required, <= 1 MB)
        MultipartFile panFile = dto.getPanCardImage();
        validateFile(panFile, "PAN card image", MAX_PAN_SIZE, true);
        String panStoredPath = storeFile(panFile, "pan_" + dto.getPanNumber().trim().toUpperCase());

        // 3. Validate GSTIN File (Optional, <= 500 KB)
        MultipartFile gstinFile = dto.getGstinPhoto();
        String gstinStoredPath = null;
        if (gstinFile != null && !gstinFile.isEmpty()) {
            validateFile(gstinFile, "GSTIN photo", MAX_GSTIN_SIZE, false);
            String prefix = StringUtils.hasText(dto.getGstin()) ? dto.getGstin().trim() : "gstin";
            gstinStoredPath = storeFile(gstinFile, "gstin_" + prefix);
        }

        // 4. Password handling (passwordless / OTP flow - random secure internal hash)
        String rawPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 5. Create Entity (auto-generated UUID id)
        User user = User.builder()
                .fullName(dto.getFullName() != null && !dto.getFullName().isBlank() ? dto.getFullName().trim() : "Buyer User")
                .phoneNumber(dto.getMobileNumber().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(encodedPassword)
                .companyName(dto.getCompanyName() != null && !dto.getCompanyName().isBlank() ? dto.getCompanyName().trim() : "KFPCL Buyer")
                .businessType(dto.getBusinessType() != null ? dto.getBusinessType() : com.project.kfpcl_exports.buyer.enums.BusinessType.WHOLESALER)
                .state(dto.getState() != null && !dto.getState().isBlank() ? dto.getState().trim() : "India")
                .city(dto.getCity() != null && !dto.getCity().isBlank() ? dto.getCity().trim() : "India")
                // GSTIN
                .gstin(StringUtils.hasText(dto.getGstin()) ? dto.getGstin().trim().toUpperCase() : null)
                .gstinPhotoUrl(gstinStoredPath)
                .gstinPhotoOriginalName(gstinFile != null && !gstinFile.isEmpty() ? StringUtils.cleanPath(gstinFile.getOriginalFilename()) : null)
                .gstinPhotoMimeType(gstinFile != null && !gstinFile.isEmpty() ? gstinFile.getContentType() : null)
                .gstinPhotoFileSize(gstinFile != null && !gstinFile.isEmpty() ? gstinFile.getSize() : null)
                // PAN
                .panNumber(dto.getPanNumber().trim().toUpperCase())
                .panCardUrl(panStoredPath)
                .panCardOriginalName(StringUtils.cleanPath(panFile.getOriginalFilename()))
                .panCardMimeType(panFile.getContentType())
                .panCardFileSize(panFile.getSize())
                // Defaults
                .role("ROLE_USER")
                .enabled(true)
                .status("PENDING_VERIFICATION")
                .build();

        User saved = buyerUserRepository.save(user);
        log.info("Buyer registered successfully in buyer_users with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    public com.project.kfpcl_exports.buyer.dto.BuyerLoginResponse loginBuyer(com.project.kfpcl_exports.buyer.dto.BuyerLoginRequest request) {
        String identifier = request.getIdentifier().trim();
        String cleanPhone = identifier.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);

        java.util.Optional<User> userOpt = java.util.Optional.empty();
        if (cleanPhone.length() == 10) {
            userOpt = buyerUserRepository.findByPhoneNumber(cleanPhone);
        }
        if (userOpt.isEmpty()) {
            userOpt = buyerUserRepository.findByEmail(identifier.toLowerCase());
        }

        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("Invalid email/phone number or password"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled. Please contact administrator");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email/phone number or password");
        }

        String accessToken = tokenService.createAccessToken(user.getId(), user.getPhoneNumber());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getPhoneNumber());

        RegistrationResponseDto profile = mapToResponse(user);
        profile.setAccessToken(accessToken);
        profile.setRefreshToken(refreshToken);

        return com.project.kfpcl_exports.buyer.dto.BuyerLoginResponse.builder()
                .success(true)
                .message("Login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .buyer(profile)
                .build();
    }

    private RegistrationResponseDto mapToResponse(User entity) {
        return RegistrationResponseDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .companyName(entity.getCompanyName())
                .businessType(entity.getBusinessType())
                .state(entity.getState())
                .city(entity.getCity())
                .gstin(entity.getGstin())
                .gstinPhotoUrl(entity.getGstinPhotoUrl())
                .panNumber(entity.getPanNumber())
                .panCardUrl(entity.getPanCardUrl())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private void validateFile(MultipartFile file, String fieldName, long maxBytes, boolean isRequired) {
        if (file == null || file.isEmpty()) {
            if (isRequired) {
                throw new IllegalArgumentException(fieldName + " is required and cannot be empty");
            }
            return;
        }
        if (file.getSize() > maxBytes) {
            String sizeLimitDesc = maxBytes >= 1_000_000 ? (maxBytes / 1_048_576L) + " MB" : (maxBytes / 1024L) + " KB";
            throw new IllegalArgumentException(fieldName + " (" + file.getSize() + " bytes) exceeds the strict " + sizeLimitDesc + " limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(fieldName + " has invalid type [" + contentType + "]. Only JPG, PNG, and PDF are accepted");
        }
    }

    private String storeFile(MultipartFile file, String prefix) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx).toLowerCase();
        }
        String safeFileName = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        if ("s3".equalsIgnoreCase(storageType)) {
            String s3Key = s3Folder + "/" + safeFileName;
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(awsS3Config.getS3Bucket())
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                return "s3://" + awsS3Config.getS3Bucket() + "/" + s3Key;
            } catch (Exception ex) {
                log.error("Failed to upload to S3, attempting fallback to local disk", ex);
                return uploadToLocalDisk(file, safeFileName);
            }
        } else {
            return uploadToLocalDisk(file, safeFileName);
        }
    }

    private String uploadToLocalDisk(MultipartFile file, String safeFileName) {
        try {
            Path targetDir = Paths.get(localUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(safeFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException ex) {
            log.error("Failed to save file locally", ex);
            throw new RuntimeException("Could not store document locally: " + ex.getMessage());
        }
    }
}
