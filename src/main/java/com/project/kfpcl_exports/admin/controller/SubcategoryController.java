package com.project.kfpcl_exports.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Subcategory;
import com.project.kfpcl_exports.admin.repository.SubcategoryRepository;
import com.project.kfpcl_exports.dto.FileUploadResponse;
import com.project.kfpcl_exports.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("adminSubcategoryController")
@RequestMapping({"/api/subcategories", "/api/admin/subcategories"})
@RequiredArgsConstructor
public class SubcategoryController {

    private static final Logger log = LoggerFactory.getLogger(SubcategoryController.class);

    private final SubcategoryRepository subcategoryRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Subcategory>> getAllSubcategories() {
        return ResponseEntity.ok(subcategoryRepository.findByDeletedFalse());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<List<Subcategory>> getSubcategoriesByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subcategoryRepository.findByCategoryIdAndDeletedFalse(categoryId));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Subcategory> getSubcategoryDetail(@PathVariable Long id) {
        return subcategoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create Subcategory via JSON payload.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Subcategory> createSubcategoryJson(@RequestBody Subcategory subcategory) {
        Subcategory saved = subcategoryRepository.save(subcategory);
        return ResponseEntity.ok(saved);
    }

    /**
     * Create Subcategory via multipart/form-data or form-urlencoded with direct image file upload.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> createSubcategoryMultipart(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "order", required = false) Integer order,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "subcategory", required = false) String subcategoryJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "subcategoryImage", required = false) MultipartFile subcategoryImage,
            HttpServletRequest request
    ) {
        try {
            Subcategory subcategory = new Subcategory();

            if (StringUtils.hasText(subcategoryJson)) {
                try {
                    subcategory = objectMapper.readValue(subcategoryJson, Subcategory.class);
                } catch (Exception e) {
                    log.warn("Could not parse subcategory JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) subcategory.setName(name);
            if (description != null) subcategory.setDescription(description);
            if (categoryId != null) subcategory.setCategoryId(categoryId);
            if (categoryName != null) subcategory.setCategoryName(categoryName);
            Integer targetOrder = getFirstNonNull(displayOrder, order, sortOrder);
            if (targetOrder != null) subcategory.setDisplayOrder(targetOrder);
            if (active != null) subcategory.setActive(active);
            if (StringUtils.hasText(imageUrl)) subcategory.setImageUrl(imageUrl);

            // Check for file upload
            MultipartFile uploadFile = getFirstNonEmpty(file, image, subcategoryImage);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "subcategories");
                subcategory.setImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage) && !StringUtils.hasText(subcategory.getImageUrl())) {
                    subcategory.setImageUrl(textImage);
                }
            }

            if (!StringUtils.hasText(subcategory.getName())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Subcategory name is required",
                        "success", false
                ));
            }

            Subcategory saved = subcategoryRepository.save(subcategory);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create subcategory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Update Subcategory via JSON payload.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Subcategory> updateSubcategoryJson(@PathVariable Long id, @RequestBody Subcategory subcategoryDetails) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isPresent()) {
            Subcategory sub = subOpt.get();
            if (subcategoryDetails.getName() != null) sub.setName(subcategoryDetails.getName());
            if (subcategoryDetails.getDescription() != null) sub.setDescription(subcategoryDetails.getDescription());
            if (subcategoryDetails.getCategoryId() != null) sub.setCategoryId(subcategoryDetails.getCategoryId());
            if (subcategoryDetails.getCategoryName() != null) sub.setCategoryName(subcategoryDetails.getCategoryName());
            if (subcategoryDetails.getDisplayOrder() != null) sub.setDisplayOrder(subcategoryDetails.getDisplayOrder());
            if (subcategoryDetails.getImageUrl() != null) sub.setImageUrl(subcategoryDetails.getImageUrl());
            if (subcategoryDetails.getActive() != null) sub.setActive(subcategoryDetails.getActive());
            Subcategory updated = subcategoryRepository.save(sub);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update Subcategory via multipart/form-data or form-urlencoded.
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> updateSubcategoryMultipart(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "order", required = false) Integer order,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "subcategory", required = false) String subcategoryJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "subcategoryImage", required = false) MultipartFile subcategoryImage,
            HttpServletRequest request
    ) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Subcategory sub = subOpt.get();

            if (StringUtils.hasText(subcategoryJson)) {
                try {
                    Subcategory parsed = objectMapper.readValue(subcategoryJson, Subcategory.class);
                    if (parsed.getName() != null) sub.setName(parsed.getName());
                    if (parsed.getDescription() != null) sub.setDescription(parsed.getDescription());
                    if (parsed.getCategoryId() != null) sub.setCategoryId(parsed.getCategoryId());
                    if (parsed.getCategoryName() != null) sub.setCategoryName(parsed.getCategoryName());
                    if (parsed.getDisplayOrder() != null) sub.setDisplayOrder(parsed.getDisplayOrder());
                    if (parsed.getActive() != null) sub.setActive(parsed.getActive());
                    if (parsed.getImageUrl() != null) sub.setImageUrl(parsed.getImageUrl());
                } catch (Exception e) {
                    log.warn("Could not parse subcategory JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) sub.setName(name);
            if (description != null) sub.setDescription(description);
            if (categoryId != null) sub.setCategoryId(categoryId);
            if (categoryName != null) sub.setCategoryName(categoryName);
            Integer targetOrder = getFirstNonNull(displayOrder, order, sortOrder);
            if (targetOrder != null) sub.setDisplayOrder(targetOrder);
            if (active != null) sub.setActive(active);
            if (StringUtils.hasText(imageUrl)) sub.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image, subcategoryImage);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "subcategories");
                sub.setImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    sub.setImageUrl(textImage);
                }
            }

            Subcategory updated = subcategoryRepository.save(sub);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update subcategory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Upload / Update Subcategory Image directly.
     */
    @PostMapping(value = {"/{id}/image", "/{id}/upload", "/{id}/upload-image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSubcategoryImage(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "subcategoryImage", required = false) MultipartFile subcategoryImage
    ) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image, subcategoryImage);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided", "success", false));
            }
            FileUploadResponse res = s3Service.uploadImage(targetFile, "subcategories");
            Subcategory sub = subOpt.get();
            sub.setImageUrl(res.getUrl());
            Subcategory saved = subcategoryRepository.save(sub);
            return ResponseEntity.ok(Map.of(
                    "message", "Subcategory image uploaded successfully",
                    "subcategory", saved,
                    "imageUrl", res.getUrl(),
                    "image", res.getUrl(),
                    "key", res.getKey(),
                    "presignedUrl", res.getPresignedUrl(),
                    "success", true
            ));
        } catch (Exception e) {
            log.error("Failed to upload subcategory image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteSubcategory(@PathVariable Long id) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isPresent()) {
            Subcategory sub = subOpt.get();
            // Delete image from S3
            if (StringUtils.hasText(sub.getImageUrl())) {
                try {
                    s3Service.deleteObject(sub.getImageUrl());
                    sub.setImageUrl(null);
                } catch (Exception e) {
                    log.warn("Failed to delete subcategory image from S3 during soft delete: {}", e.getMessage());
                }
            }
            sub.setDeleted(true);
            subcategoryRepository.save(sub);
            return ResponseEntity.ok(Map.of("message", "Subcategory soft deleted and image removed from S3", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Map<String, Object>> hardDeleteSubcategory(@PathVariable Long id) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isPresent()) {
            Subcategory subcategory = subOpt.get();
            if (StringUtils.hasText(subcategory.getImageUrl())) {
                try {
                    s3Service.deleteObject(subcategory.getImageUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete subcategory image from S3: {}", e.getMessage());
                }
            }
            subcategoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Subcategory permanently deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    private MultipartFile getFirstNonEmpty(MultipartFile... files) {
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                return f;
            }
        }
        return null;
    }

    private Integer getFirstNonNull(Integer... values) {
        for (Integer v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
