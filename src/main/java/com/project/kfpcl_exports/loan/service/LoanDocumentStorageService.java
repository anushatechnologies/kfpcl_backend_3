package com.project.kfpcl_exports.loan.service;

import com.project.kfpcl_exports.config.AwsS3Config;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(LoanDocumentStorageService.class);

    private static final long MAX_DOCUMENT_SIZE_BYTES = 15 * 1024 * 1024; // 15 MB limit per document
    private static final int DEFAULT_PRESIGNED_EXPIRY_MINUTES = 15; // 15 minutes short-lived URL

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3Config awsS3Config;

    public record StoredDocumentResult(
            String objectKey,
            String category,
            String originalFilename,
            String contentType,
            long fileSize,
            LocalDateTime uploadTimestamp,
            String uploadStatus
    ) {}

    /**
     * Upload loan application document to existing private Supabase S3 bucket.
     * Uses generated object names without sensitive PII.
     * Path format: loan-applications/{applicationId}/{category}/{uniqueFileId}.{ext}
     */
    public StoredDocumentResult uploadDocument(String applicationId, String category, MultipartFile file) throws IOException {
        validateDocumentFile(file, category);

        String bucket = awsS3Config.getS3Bucket();
        String cleanCategory = sanitizeCategory(category);
        String uniqueFileId = UUID.randomUUID().toString();
        String extension = determineExtension(file);

        // e.g. loan-applications/{applicationId}/aadhaar/{uniqueFileId}.pdf
        String objectKey = String.format("loan-applications/%s/%s/%s%s", applicationId, cleanCategory, uniqueFileId, extension);
        String contentType = detectContentType(file, extension);

        log.info("Uploading loan document to Supabase Storage. Category: '{}', Key: '{}', Size: {} bytes, Content-Type: '{}'",
                cleanCategory, objectKey, file.getSize(), contentType);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        PutObjectResponse response = s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        if (response == null || !response.sdkHttpResponse().isSuccessful()) {
            throw new IOException("Storage upload failed: Supabase did not acknowledge successful receipt for " + objectKey);
        }

        log.info("Storage confirmed success for loan document key: '{}'", objectKey);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document" + extension);

        return new StoredDocumentResult(
                objectKey,
                cleanCategory,
                originalFilename,
                contentType,
                file.getSize(),
                LocalDateTime.now(),
                "UPLOADED"
        );
    }

    /**
     * Delete an object from Supabase bucket (used for rollback on partial failure).
     */
    public void deleteObject(String objectKey) {
        if (!StringUtils.hasText(objectKey)) return;
        try {
            String bucket = awsS3Config.getS3Bucket();
            log.info("Deleting object from Supabase bucket: '{}', key: '{}'", bucket, objectKey);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully requested deletion of key: '{}'", objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete object from bucket (key: '{}'): {}", objectKey, e.getMessage());
        }
    }

    /**
     * Delete multiple objects (used when rolling back failed submissions).
     */
    public void deleteObjects(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) return;
        for (String key : objectKeys) {
            deleteObject(key);
        }
    }

    /**
     * Download / stream object from private bucket.
     */
    public ResponseInputStream<GetObjectResponse> getObjectStream(String objectKey) {
        String bucket = awsS3Config.getS3Bucket();
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        return s3Client.getObject(getRequest);
    }

    /**
     * Read full object bytes from private bucket.
     */
    public byte[] getObjectBytes(String objectKey) throws IOException {
        try (ResponseInputStream<GetObjectResponse> stream = getObjectStream(objectKey)) {
            return stream.readAllBytes();
        }
    }

    /**
     * Generate short-lived presigned GET URL for secure temporary download.
     */
    public String generatePresignedDownloadUrl(String objectKey, int expiryMinutes) {
        String bucket = awsS3Config.getS3Bucket();
        int minutes = (expiryMinutes > 0 && expiryMinutes <= 120) ? expiryMinutes : DEFAULT_PRESIGNED_EXPIRY_MINUTES;

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(minutes))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    private void validateDocumentFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file for category '" + category + "' is empty or not provided.");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "File size exceeds %d MB limit for '%s'. Actual size: %.2f MB",
                    MAX_DOCUMENT_SIZE_BYTES / (1024 * 1024),
                    category,
                    file.getSize() / (1024.0 * 1024.0)
            ));
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean validMime = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean validExt = originalFilename != null && (
                originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf") ||
                originalFilename.toLowerCase(Locale.ROOT).endsWith(".jpg") ||
                originalFilename.toLowerCase(Locale.ROOT).endsWith(".jpeg") ||
                originalFilename.toLowerCase(Locale.ROOT).endsWith(".png") ||
                originalFilename.toLowerCase(Locale.ROOT).endsWith(".webp")
        );

        if (!validMime && !validExt) {
            throw new IllegalArgumentException(
                    "Invalid file format for category '" + category + "'. Only PDF, JPEG, PNG, and WebP are allowed. Provided: " + contentType
            );
        }
    }

    private String sanitizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "general";
        }
        String clean = category.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]", "-");
        return clean.isEmpty() ? "general" : clean;
    }

    private String determineExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            int dot = original.lastIndexOf('.');
            String ext = original.substring(dot).toLowerCase(Locale.ROOT);
            if (ext.equals(".pdf") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
                return ext;
            }
        }
        String contentType = file.getContentType();
        if ("application/pdf".equalsIgnoreCase(contentType)) return ".pdf";
        if ("image/jpeg".equalsIgnoreCase(contentType)) return ".jpg";
        if ("image/png".equalsIgnoreCase(contentType)) return ".png";
        if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
        return ".pdf";
    }

    private String detectContentType(MultipartFile file, String extension) {
        String ct = file.getContentType();
        if (StringUtils.hasText(ct) && ALLOWED_CONTENT_TYPES.contains(ct.toLowerCase(Locale.ROOT))) {
            return ct;
        }
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
