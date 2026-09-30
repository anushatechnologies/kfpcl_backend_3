package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.config.AwsS3Config;
import com.project.kfpcl_exports.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // Allow uploads up to 25 MB before compression
    private static final long TARGET_MAX_COMPRESSED_BYTES = 200 * 1024; // Target size <= 200 KB
    private static final int DEFAULT_PRESIGNED_EXPIRY_MINUTES = 60;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml",
            "image/bmp",
            "image/tiff"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3Config awsS3Config;

    /**
     * Upload an image file to the private S3 bucket with automatic compression to <= 200 KB.
     *
     * @param file   MultipartFile image to upload
     * @param folder Subfolder to group images (e.g. products, categories, banners)
     * @return FileUploadResponse containing S3 key, backend proxy URL, and presigned URL
     */
    public FileUploadResponse uploadImage(MultipartFile file, String folder) throws IOException {
        validateImageFile(file);

        String bucket = awsS3Config.getS3Bucket();
        String sanitizedFolder = sanitizeFolder(folder);

        // Automatically compress image down to <= 200 KB
        CompressedImageResult compressed = compressImageToTarget(file);
        byte[] uploadBytes = compressed.getBytes();
        String contentType = compressed.getContentType();
        String uniqueKey = generateObjectKey(file, sanitizedFolder, compressed.isConvertedToJpg());

        log.info("Uploading image to S3. Bucket: '{}', Key: '{}', Original Size: {} KB, Compressed Size: {} KB, Content-Type: '{}'",
                bucket, uniqueKey, file.getSize() / 1024, uploadBytes.length / 1024, contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueKey)
                .contentType(contentType)
                .contentLength((long) uploadBytes.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(uploadBytes));

        log.info("Successfully uploaded compressed image to S3 key: '{}' ({} KB)", uniqueKey, uploadBytes.length / 1024);

        // Generate permanent backend streaming URL
        String backendAccessUrl = "/api/media/view?key=" + uniqueKey;

        // Generate temporary direct S3 presigned URL (valid for 60 minutes)
        String presignedUrl = generatePresignedGetUrl(uniqueKey, DEFAULT_PRESIGNED_EXPIRY_MINUTES);

        return FileUploadResponse.builder()
                .key(uniqueKey)
                .url(backendAccessUrl)
                .presignedUrl(presignedUrl)
                .fileName(file.getOriginalFilename())
                .fileSize((long) uploadBytes.length)
                .contentType(contentType)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Download/Stream a private S3 object.
     *
     * @param objectKey S3 object key or URL containing key
     * @return S3 ResponseInputStream containing image data and metadata
     */
    public ResponseInputStream<GetObjectResponse> getObjectStream(String objectKey) {
        String cleanKey = extractObjectKey(objectKey);
        String bucket = awsS3Config.getS3Bucket();

        log.debug("Fetching S3 object. Bucket: '{}', Key: '{}'", bucket, cleanKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(cleanKey)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    /**
     * Generate a temporary presigned GET URL for direct download from S3.
     *
     * @param objectKey     S3 object key or URL containing key
     * @param expiryMinutes Validity duration in minutes
     * @return Presigned GET URL string
     */
    public String generatePresignedGetUrl(String objectKey, int expiryMinutes) {
        String cleanKey = extractObjectKey(objectKey);
        String bucket = awsS3Config.getS3Bucket();

        if (expiryMinutes <= 0) {
            expiryMinutes = DEFAULT_PRESIGNED_EXPIRY_MINUTES;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(cleanKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    /**
     * Delete an object from the S3 bucket.
     *
     * @param objectKey S3 object key or URL containing key
     */
    public void deleteObject(String objectKey) {
        String cleanKey = extractObjectKey(objectKey);
        String bucket = awsS3Config.getS3Bucket();

        log.info("Deleting S3 object. Bucket: '{}', Key: '{}'", bucket, cleanKey);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(cleanKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("Successfully requested deletion of S3 object key: '{}'", cleanKey);
    }

    /**
     * Validate that the file is not empty, content type is an allowed image format, and size <= 10 MB.
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format("File size exceeds 10 MB limit. Actual size: %.2f MB",
                    file.getSize() / (1024.0 * 1024.0)));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid file type. Only image files (JPEG, PNG, WebP, GIF, SVG, BMP, TIFF) are allowed. Provided: " + contentType);
        }
    }

    /**
     * Helper to store compressed image result.
     */
    private static class CompressedImageResult {
        private final byte[] bytes;
        private final String contentType;
        private final boolean convertedToJpg;

        public CompressedImageResult(byte[] bytes, String contentType, boolean convertedToJpg) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.convertedToJpg = convertedToJpg;
        }

        public byte[] getBytes() { return bytes; }
        public String getContentType() { return contentType; }
        public boolean isConvertedToJpg() { return convertedToJpg; }
    }

    /**
     * Compress image down to <= 200 KB while maintaining crisp visual quality.
     */
    private CompressedImageResult compressImageToTarget(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "image/jpeg";
        }

        // SVG or animated GIF should not be recompressed as JPEG
        if ("image/svg+xml".equalsIgnoreCase(contentType) || "image/gif".equalsIgnoreCase(contentType)) {
            try {
                return new CompressedImageResult(file.getBytes(), contentType, false);
            } catch (IOException e) {
                log.warn("Failed to read SVG/GIF bytes: {}", e.getMessage());
            }
        }

        try {
            byte[] originalBytes = file.getBytes();

            // If file is already <= 200 KB, keep original unless it's an oversized uncompressed bitmap
            if (originalBytes.length <= TARGET_MAX_COMPRESSED_BYTES && !contentType.equalsIgnoreCase("image/bmp")) {
                return new CompressedImageResult(originalBytes, contentType, false);
            }

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (originalImage == null) {
                // Not a raster format recognizable by ImageIO, return original
                return new CompressedImageResult(originalBytes, contentType, false);
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // Downscale dimensions if larger than 1400px (1400px is crystal clear for web/mobile)
            int maxDim = 1400;
            int targetWidth = width;
            int targetHeight = height;
            if (targetWidth > maxDim || targetHeight > maxDim) {
                double ratio = (double) width / height;
                if (ratio > 1) {
                    targetWidth = maxDim;
                    targetHeight = (int) (maxDim / ratio);
                } else {
                    targetHeight = maxDim;
                    targetWidth = (int) (maxDim * ratio);
                }
            }

            // Progressively compress until <= 200 KB
            float[] qualities = {0.85f, 0.75f, 0.65f, 0.55f, 0.45f};
            byte[] bestResult = originalBytes;

            for (float quality : qualities) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.of(originalImage)
                        .size(targetWidth, targetHeight)
                        .outputQuality(quality)
                        .outputFormat("jpg")
                        .toOutputStream(baos);

                byte[] compressedBytes = baos.toByteArray();
                bestResult = compressedBytes;

                if (compressedBytes.length <= TARGET_MAX_COMPRESSED_BYTES) {
                    log.info("Successfully compressed image to {} KB with quality {} (original: {} KB)",
                            compressedBytes.length / 1024, quality, originalBytes.length / 1024);
                    return new CompressedImageResult(compressedBytes, "image/jpeg", true);
                }

                // If still above 200KB, reduce dimensions by 15% for next iteration
                targetWidth = (int) (targetWidth * 0.85);
                targetHeight = (int) (targetHeight * 0.85);
                if (targetWidth < 300 || targetHeight < 300) {
                    break;
                }
            }

            log.info("Compressed image from {} KB to {} KB", originalBytes.length / 1024, bestResult.length / 1024);
            return new CompressedImageResult(bestResult, "image/jpeg", true);

        } catch (Exception e) {
            log.warn("Image compression failed, falling back to original file: {}", e.getMessage());
            try {
                return new CompressedImageResult(file.getBytes(), contentType, false);
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to read image bytes: " + ioException.getMessage(), ioException);
            }
        }
    }

    /**
     * Generate a unique S3 object key with correct extension.
     */
    private String generateObjectKey(MultipartFile file, String folder, boolean convertedToJpg) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = "image.jpg";
        }
        String cleanFilename = StringUtils.cleanPath(originalFilename).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (convertedToJpg) {
            int lastDot = cleanFilename.lastIndexOf('.');
            if (lastDot != -1) {
                cleanFilename = cleanFilename.substring(0, lastDot) + ".jpg";
            } else {
                cleanFilename = cleanFilename + ".jpg";
            }
        }
        return String.format("media/%s/%s-%s", folder, UUID.randomUUID(), cleanFilename);
    }

    /**
     * Sanitize folder name.
     */
    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return "general";
        }
        String clean = folder.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return clean.isEmpty() ? "general" : clean;
    }

    /**
     * Extract the raw object key if a URL or parameter string was provided.
     */
    public String extractObjectKey(String input) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("Object key or URL cannot be empty");
        }

        String decoded = URLDecoder.decode(input.trim(), StandardCharsets.UTF_8);

        // If it's a backend view URL like /api/media/view?key=media/products/abc.jpg
        if (decoded.contains("key=")) {
            int keyIndex = decoded.indexOf("key=");
            String keyPart = decoded.substring(keyIndex + 4);
            int ampIndex = keyPart.indexOf("&");
            if (ampIndex != -1) {
                keyPart = keyPart.substring(0, ampIndex);
            }
            return keyPart;
        }

        // If it's a direct S3 URL like https://bucket.s3.region.amazonaws.com/media/products/abc.jpg
        if (decoded.contains(".amazonaws.com/")) {
            return decoded.substring(decoded.indexOf(".amazonaws.com/") + 15);
        }

        // If it's an s3:// URI like s3://bucket-name/buyer_documents/pan_123.jpg
        if (decoded.startsWith("s3://")) {
            int slashIndex = decoded.indexOf('/', 5);
            if (slashIndex != -1) {
                return decoded.substring(slashIndex + 1);
            }
        }

        // Remove any leading slash
        if (decoded.startsWith("/")) {
            decoded = decoded.substring(1);
        }

        return decoded;
    }
}
