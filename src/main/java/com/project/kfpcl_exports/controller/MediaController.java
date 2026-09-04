package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.FileUploadResponse;
import com.project.kfpcl_exports.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping({
        "/api/media",
        "/api/admin/media",
        "/api/upload",
        "/api/admin/upload",
        "/api/image",
        "/api/images",
        "/api/products/upload",
        "/api/categories/upload",
        "/api/subcategories/upload",
        "/api/sections/upload",
        "/api/section/upload"
})
@RequiredArgsConstructor
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final S3Service s3Service;

    /**
     * Upload an image to S3 via POST /api/media/upload or /api/upload.
     * Supports form parameters: 'file' or 'image' (MultipartFile)
     * Supports folder or type parameter: 'category', 'subcategory', 'products', 'banners', 'store' (or uppercase equivalents)
     */
    @PostMapping(value = {"/upload", "/image", ""}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "folder", required = false) String folderParam,
            @RequestParam(value = "type", required = false) String typeParam
    ) {
        try {
            MultipartFile targetFile = (file != null && !file.isEmpty()) ? file : image;
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No file provided. Please attach a file under form field 'file' or 'image'.",
                        "success", false
                ));
            }

            String targetFolder = "products";
            String rawType = (typeParam != null && !typeParam.trim().isEmpty()) ? typeParam.trim() : folderParam;
            if (rawType != null && !rawType.trim().isEmpty()) {
                String t = rawType.trim().toLowerCase();
                targetFolder = t.endsWith("s") ? t : t + "s";
            }

            FileUploadResponse response = s3Service.uploadImage(targetFile, targetFolder);

            Map<String, Object> dataMap = Map.of(
                    "url", response.getUrl(),
                    "storageKey", response.getKey(),
                    "key", response.getKey(),
                    "imageUrl", response.getUrl()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Image uploaded successfully",
                    "url", response.getUrl(),
                    "imageUrl", response.getUrl(),
                    "image", response.getUrl(),
                    "key", response.getKey(),
                    "storageKey", response.getKey(),
                    "data", dataMap,
                    "fileUploadResponse", response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        } catch (Exception e) {
            log.error("Failed to upload image to S3: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Image upload failed: " + e.getMessage(), "success", false));
        }
    }

    /**
     * Upload product image.
     */
    @PostMapping(value = "/products/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return uploadFile(file, image, "products", "products");
    }

    /**
     * Upload category image.
     */
    @PostMapping(value = "/categories/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCategoryImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return uploadFile(file, image, "categories", "categories");
    }

    /**
     * Upload subcategory image.
     */
    @PostMapping(value = "/subcategories/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSubcategoryImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return uploadFile(file, image, "subcategories", "subcategories");
    }

    /**
     * Upload banner image.
     */
    @PostMapping(value = "/banners/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBannerImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return uploadFile(file, image, "banners", "banners");
    }

    /**
     * Upload store image.
     */
    @PostMapping(value = "/stores/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStoreImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return uploadFile(file, image, "stores", "stores");
    }

    /**
     * Upload section image.
     */
    @PostMapping(value = {"/sections/upload-image", "/section/upload-image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSectionImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "sectionImage", required = false) MultipartFile sectionImage
    ) {
        MultipartFile targetFile = (file != null && !file.isEmpty()) ? file : ((image != null && !image.isEmpty()) ? image : sectionImage);
        return uploadFile(targetFile, null, "sections", "sections");
    }

    /**
     * Stream a private S3 image object through the backend with caching headers.
     * Permanent URL compatible with frontend image rendering: GET /api/media/view?key=...
     */
    @GetMapping("/view")
    public ResponseEntity<?> viewFile(@RequestParam("key") String key) {
        try {
            ResponseInputStream<GetObjectResponse> s3Stream = s3Service.getObjectStream(key);
            GetObjectResponse s3Response = s3Stream.response();

            byte[] content = s3Stream.readAllBytes();

            String contentType = s3Response.contentType();
            MediaType mediaType;
            try {
                mediaType = (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(content.length);
            headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic().getHeaderValue());
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + s3Service.extractObjectKey(key) + "\"");

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (NoSuchKeyException e) {
            log.warn("S3 object not found for key: {}", key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found in S3", "key", key));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving S3 object for key {}: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve image: " + e.getMessage()));
        }
    }

    /**
     * Generate an on-demand temporary presigned GET URL for direct S3 download.
     * GET /api/media/presigned-url?key=...&expiryMinutes=60
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPresignedUrl(
            @RequestParam("key") String key,
            @RequestParam(value = "expiryMinutes", required = false, defaultValue = "60") int expiryMinutes
    ) {
        try {
            String presignedUrl = s3Service.generatePresignedGetUrl(key, expiryMinutes);
            return ResponseEntity.ok(Map.of(
                    "key", s3Service.extractObjectKey(key),
                    "presignedUrl", presignedUrl,
                    "expiryMinutes", expiryMinutes
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating presigned URL for key {}: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate presigned URL: " + e.getMessage()));
        }
    }

    /**
     * Delete an S3 object.
     * DELETE /api/media/delete?key=... or DELETE /api/admin/media/delete?key=...
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("key") String key) {
        try {
            s3Service.deleteObject(key);
            return ResponseEntity.ok(Map.of(
                    "message", "File deleted successfully",
                    "key", s3Service.extractObjectKey(key),
                    "success", true
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting S3 object for key {}: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage(), "success", false));
        }
    }
}
