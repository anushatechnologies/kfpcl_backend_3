package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.FileUploadResponse;
import com.project.kfpcl_exports.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping({
        "/api/v1/admin/catalog/images",
        "/api/admin/catalog/images",
        "/api/catalog/images",
        "/api/v1/catalog/images"
})
@RequiredArgsConstructor
public class CatalogImageController {

    private static final Logger log = LoggerFactory.getLogger(CatalogImageController.class);
    private final S3Service s3Service;

    /**
     * Catalog Image Upload API
     * Supports POST /api/v1/admin/catalog/images and /api/admin/catalog/images
     * Content-Type: multipart/form-data
     * Request fields: 'file' or 'image' or 'subcategoryImage' or 'categoryImage'
     * Optional 'type' param: e.g. SUBCATEGORY, CATEGORY, PRODUCT, BANNER, STORE
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCatalogImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "subcategoryImage", required = false) MultipartFile subcategoryImage,
            @RequestParam(value = "categoryImage", required = false) MultipartFile categoryImage,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage,
            @RequestParam(value = "sectionImage", required = false) MultipartFile sectionImage,
            @RequestParam(value = "type", required = false, defaultValue = "catalog") String type
    ) {
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image, subcategoryImage, categoryImage, productImage, sectionImage);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No file provided. Please attach a file under form field 'file', 'image', 'subcategoryImage', 'categoryImage', or 'productImage'."
                ));
            }

            String folder = "catalog";
            if (type != null && !type.trim().isEmpty()) {
                String t = type.trim().toLowerCase();
                folder = t.endsWith("s") ? t : t + "s";
            }

            FileUploadResponse uploadRes = s3Service.uploadImage(targetFile, folder);

            Map<String, Object> dataMap = Map.of(
                    "url", uploadRes.getUrl(),
                    "storageKey", uploadRes.getKey(),
                    "key", uploadRes.getKey(),
                    "imageUrl", uploadRes.getUrl()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Catalog image uploaded successfully",
                    "data", dataMap,
                    "url", uploadRes.getUrl(),
                    "storageKey", uploadRes.getKey(),
                    "key", uploadRes.getKey(),
                    "imageUrl", uploadRes.getUrl()
            ));
        } catch (Exception e) {
            log.error("Failed to upload catalog image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Image upload failed: " + e.getMessage()
                    ));
        }
    }

    private MultipartFile getFirstNonEmpty(MultipartFile... files) {
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                return f;
            }
        }
        return null;
    }
}
