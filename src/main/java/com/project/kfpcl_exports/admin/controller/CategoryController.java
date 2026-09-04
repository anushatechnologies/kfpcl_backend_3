package com.project.kfpcl_exports.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Category;
import com.project.kfpcl_exports.admin.repository.CategoryRepository;
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

@RestController("adminCategoryController")
@RequestMapping({"/api/categories", "/api/admin/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findByDeletedFalse());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create Category via JSON payload.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Category> createCategoryJson(@RequestBody Category category) {
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(saved);
    }

    /**
     * Create Category via multipart/form-data or form-urlencoded with direct image file upload.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> createCategoryMultipart(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "discount", required = false) Double discount,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "category", required = false) String categoryJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "categoryImage", required = false) MultipartFile categoryImage,
            HttpServletRequest request
    ) {
        try {
            Category category = new Category();

            if (StringUtils.hasText(categoryJson)) {
                try {
                    category = objectMapper.readValue(categoryJson, Category.class);
                } catch (Exception e) {
                    log.warn("Could not parse category JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) category.setName(name);
            if (description != null) category.setDescription(description);
            if (discount != null) category.setDiscount(discount);
            if (active != null) category.setActive(active);
            if (StringUtils.hasText(imageUrl)) category.setImageUrl(imageUrl);

            // Check for file upload
            MultipartFile uploadFile = getFirstNonEmpty(file, image, categoryImage);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "categories");
                category.setImageUrl(uploadRes.getUrl());
            }

            if (!StringUtils.hasText(category.getName())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Category name is required",
                        "success", false
                ));
            }

            Category saved = categoryRepository.save(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Update Category via JSON payload.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Category> updateCategoryJson(@PathVariable Long id, @RequestBody Category categoryDetails) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isPresent()) {
            Category category = catOpt.get();
            if (categoryDetails.getName() != null) category.setName(categoryDetails.getName());
            if (categoryDetails.getDescription() != null) category.setDescription(categoryDetails.getDescription());
            if (categoryDetails.getImageUrl() != null) category.setImageUrl(categoryDetails.getImageUrl());
            if (categoryDetails.getDiscount() != null) category.setDiscount(categoryDetails.getDiscount());
            if (categoryDetails.getActive() != null) category.setActive(categoryDetails.getActive());
            Category updated = categoryRepository.save(category);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update Category via multipart/form-data or form-urlencoded.
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> updateCategoryMultipart(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "discount", required = false) Double discount,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "category", required = false) String categoryJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "categoryImage", required = false) MultipartFile categoryImage,
            HttpServletRequest request
    ) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Category category = catOpt.get();

            if (StringUtils.hasText(categoryJson)) {
                try {
                    Category parsed = objectMapper.readValue(categoryJson, Category.class);
                    if (parsed.getName() != null) category.setName(parsed.getName());
                    if (parsed.getDescription() != null) category.setDescription(parsed.getDescription());
                    if (parsed.getDiscount() != null) category.setDiscount(parsed.getDiscount());
                    if (parsed.getActive() != null) category.setActive(parsed.getActive());
                    if (parsed.getImageUrl() != null) category.setImageUrl(parsed.getImageUrl());
                } catch (Exception e) {
                    log.warn("Could not parse category JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) category.setName(name);
            if (description != null) category.setDescription(description);
            if (discount != null) category.setDiscount(discount);
            if (active != null) category.setActive(active);
            if (StringUtils.hasText(imageUrl)) category.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image, categoryImage);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "categories");
                category.setImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    category.setImageUrl(textImage);
                }
            }

            Category updated = categoryRepository.save(category);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update category: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Upload / Update Category Image directly.
     */
    @PostMapping(value = {"/{id}/image", "/{id}/upload", "/{id}/upload-image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCategoryImage(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "categoryImage", required = false) MultipartFile categoryImage
    ) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image, categoryImage);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided", "success", false));
            }
            FileUploadResponse res = s3Service.uploadImage(targetFile, "categories");
            Category category = catOpt.get();
            category.setImageUrl(res.getUrl());
            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(Map.of(
                    "message", "Category image uploaded successfully",
                    "category", saved,
                    "imageUrl", res.getUrl(),
                    "image", res.getUrl(),
                    "key", res.getKey(),
                    "presignedUrl", res.getPresignedUrl(),
                    "success", true
            ));
        } catch (Exception e) {
            log.error("Failed to upload category image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteCategory(@PathVariable Long id) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isPresent()) {
            Category category = catOpt.get();
            category.setDeleted(true);
            categoryRepository.save(category);
            return ResponseEntity.ok(Map.of("message", "Category soft deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Map<String, Object>> hardDeleteCategory(@PathVariable Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Category permanently deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Category>> searchCategories(@RequestParam String keyword) {
        return ResponseEntity.ok(categoryRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword));
    }

    @GetMapping("/filter/discount")
    public ResponseEntity<List<Category>> filterByDiscount(@RequestParam Double minDiscount) {
        return ResponseEntity.ok(categoryRepository.findByDiscountGreaterThanEqualAndDeletedFalse(minDiscount));
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
