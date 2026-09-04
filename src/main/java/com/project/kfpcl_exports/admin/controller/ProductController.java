package com.project.kfpcl_exports.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.admin.model.ProductImage;
import com.project.kfpcl_exports.admin.repository.ProductImageRepository;
import com.project.kfpcl_exports.admin.repository.ProductRepository;
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

@RestController("adminProductController")
@RequestMapping({"/api/products", "/api/admin/products"})
@RequiredArgsConstructor
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create Product via JSON payload.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> createProductJson(@RequestBody Product product) {
        if (StringUtils.hasText(product.getMainImageUrl())) {
            if (product.getImages() == null || product.getImages().isEmpty()) {
                ProductImage primaryImg = ProductImage.builder()
                        .imageUrl(product.getMainImageUrl())
                        .isPrimary(true)
                        .product(product)
                        .build();
                product.getImages().add(primaryImg);
            }
        }
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Create Product via multipart/form-data or form-urlencoded with direct image file upload.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> createProductMultipart(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "originalPrice", required = false) Double originalPrice,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "subcategoryId", required = false) Long subcategoryId,
            @RequestParam(value = "subcategoryName", required = false) String subcategoryName,
            @RequestParam(value = "mainImageUrl", required = false) String mainImageUrl,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "trending", required = false) Boolean trending,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "product", required = false) String productJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage,
            @RequestParam(value = "images", required = false) List<MultipartFile> imagesList,
            HttpServletRequest request
    ) {
        try {
            Product product = new Product();

            if (StringUtils.hasText(productJson)) {
                try {
                    product = objectMapper.readValue(productJson, Product.class);
                } catch (Exception e) {
                    log.warn("Could not parse product JSON parameter: {}", e.getMessage());
                }
            }

            String finalTitle = StringUtils.hasText(title) ? title : name;
            if (StringUtils.hasText(finalTitle)) product.setTitle(finalTitle);
            if (description != null) product.setDescription(description);
            if (price != null) product.setPrice(price);
            if (originalPrice != null) product.setOriginalPrice(originalPrice);
            if (stock != null) product.setStock(stock);
            if (unit != null) product.setUnit(unit);
            if (categoryId != null) product.setCategoryId(categoryId);
            if (categoryName != null) product.setCategoryName(categoryName);
            if (subcategoryId != null) product.setSubcategoryId(subcategoryId);
            if (subcategoryName != null) product.setSubcategoryName(subcategoryName);
            if (trending != null) product.setTrending(trending);
            if (active != null) product.setActive(active);

            String textUrl = StringUtils.hasText(mainImageUrl) ? mainImageUrl : imageUrl;
            if (StringUtils.hasText(textUrl)) product.setMainImageUrl(textUrl);

            // Check for uploaded main image file
            MultipartFile mainFile = getFirstNonEmpty(file, image, mainImage, productImage);
            if (mainFile != null && !mainFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(mainFile, "products");
                product.setMainImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage) && !StringUtils.hasText(product.getMainImageUrl())) {
                    product.setMainImageUrl(textImage);
                }
            }

            if (!StringUtils.hasText(product.getTitle())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Product title is required",
                        "success", false
                ));
            }

            // If mainImageUrl is set, ensure primary ProductImage exists
            if (StringUtils.hasText(product.getMainImageUrl())) {
                boolean hasPrimary = product.getImages().stream().anyMatch(i -> Boolean.TRUE.equals(i.getIsPrimary()));
                if (!hasPrimary) {
                    ProductImage primaryImg = ProductImage.builder()
                            .imageUrl(product.getMainImageUrl())
                            .isPrimary(true)
                            .product(product)
                            .build();
                    product.getImages().add(primaryImg);
                }
            }

            // Check for additional gallery images
            if (request instanceof MultipartHttpServletRequest multipartRequest) {
                List<MultipartFile> galleryFiles = multipartRequest.getFiles("images");
                if (galleryFiles.isEmpty()) {
                    galleryFiles = multipartRequest.getFiles("galleryImages");
                }
                for (MultipartFile gf : galleryFiles) {
                    if (gf != null && !gf.isEmpty() && !gf.equals(mainFile)) {
                        FileUploadResponse gRes = s3Service.uploadImage(gf, "products");
                        ProductImage gImg = ProductImage.builder()
                                .imageUrl(gRes.getUrl())
                                .isPrimary(false)
                                .product(product)
                                .build();
                        product.getImages().add(gImg);
                    }
                }
            }

            Product saved = productRepository.save(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create product: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Update Product via JSON payload.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> updateProductJson(@PathVariable Long id, @RequestBody Product productDetails) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isPresent()) {
            Product product = pOpt.get();
            if (productDetails.getTitle() != null) product.setTitle(productDetails.getTitle());
            if (productDetails.getDescription() != null) product.setDescription(productDetails.getDescription());
            if (productDetails.getPrice() != null) product.setPrice(productDetails.getPrice());
            if (productDetails.getOriginalPrice() != null) product.setOriginalPrice(productDetails.getOriginalPrice());
            if (productDetails.getStock() != null) product.setStock(productDetails.getStock());
            if (productDetails.getUnit() != null) product.setUnit(productDetails.getUnit());
            if (productDetails.getCategoryId() != null) product.setCategoryId(productDetails.getCategoryId());
            if (productDetails.getCategoryName() != null) product.setCategoryName(productDetails.getCategoryName());
            if (productDetails.getSubcategoryId() != null) product.setSubcategoryId(productDetails.getSubcategoryId());
            if (productDetails.getSubcategoryName() != null) product.setSubcategoryName(productDetails.getSubcategoryName());
            if (productDetails.getMainImageUrl() != null) product.setMainImageUrl(productDetails.getMainImageUrl());
            if (productDetails.getTrending() != null) product.setTrending(productDetails.getTrending());
            if (productDetails.getActive() != null) product.setActive(productDetails.getActive());

            Product updated = productRepository.save(product);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update Product via multipart/form-data or form-urlencoded.
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> updateProductMultipart(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "originalPrice", required = false) Double originalPrice,
            @RequestParam(value = "stock", required = false) Integer stock,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "subcategoryId", required = false) Long subcategoryId,
            @RequestParam(value = "subcategoryName", required = false) String subcategoryName,
            @RequestParam(value = "mainImageUrl", required = false) String mainImageUrl,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "trending", required = false) Boolean trending,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "product", required = false) String productJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage,
            HttpServletRequest request
    ) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Product product = pOpt.get();

            if (StringUtils.hasText(productJson)) {
                try {
                    Product parsed = objectMapper.readValue(productJson, Product.class);
                    if (parsed.getTitle() != null) product.setTitle(parsed.getTitle());
                    if (parsed.getDescription() != null) product.setDescription(parsed.getDescription());
                    if (parsed.getPrice() != null) product.setPrice(parsed.getPrice());
                    if (parsed.getOriginalPrice() != null) product.setOriginalPrice(parsed.getOriginalPrice());
                    if (parsed.getStock() != null) product.setStock(parsed.getStock());
                    if (parsed.getUnit() != null) product.setUnit(parsed.getUnit());
                    if (parsed.getCategoryId() != null) product.setCategoryId(parsed.getCategoryId());
                    if (parsed.getCategoryName() != null) product.setCategoryName(parsed.getCategoryName());
                    if (parsed.getSubcategoryId() != null) product.setSubcategoryId(parsed.getSubcategoryId());
                    if (parsed.getSubcategoryName() != null) product.setSubcategoryName(parsed.getSubcategoryName());
                    if (parsed.getMainImageUrl() != null) product.setMainImageUrl(parsed.getMainImageUrl());
                    if (parsed.getTrending() != null) product.setTrending(parsed.getTrending());
                    if (parsed.getActive() != null) product.setActive(parsed.getActive());
                } catch (Exception e) {
                    log.warn("Could not parse product JSON parameter: {}", e.getMessage());
                }
            }

            String finalTitle = StringUtils.hasText(title) ? title : name;
            if (StringUtils.hasText(finalTitle)) product.setTitle(finalTitle);
            if (description != null) product.setDescription(description);
            if (price != null) product.setPrice(price);
            if (originalPrice != null) product.setOriginalPrice(originalPrice);
            if (stock != null) product.setStock(stock);
            if (unit != null) product.setUnit(unit);
            if (categoryId != null) product.setCategoryId(categoryId);
            if (categoryName != null) product.setCategoryName(categoryName);
            if (subcategoryId != null) product.setSubcategoryId(subcategoryId);
            if (subcategoryName != null) product.setSubcategoryName(subcategoryName);
            if (trending != null) product.setTrending(trending);
            if (active != null) product.setActive(active);

            String textUrl = StringUtils.hasText(mainImageUrl) ? mainImageUrl : imageUrl;
            if (StringUtils.hasText(textUrl)) product.setMainImageUrl(textUrl);

            MultipartFile mainFile = getFirstNonEmpty(file, image, mainImage, productImage);
            if (mainFile != null && !mainFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(mainFile, "products");
                product.setMainImageUrl(uploadRes.getUrl());

                // Update primary ProductImage if present, or add one
                boolean updatedPrimary = false;
                for (ProductImage img : product.getImages()) {
                    if (Boolean.TRUE.equals(img.getIsPrimary())) {
                        img.setImageUrl(uploadRes.getUrl());
                        updatedPrimary = true;
                        break;
                    }
                }
                if (!updatedPrimary) {
                    ProductImage primaryImg = ProductImage.builder()
                            .imageUrl(uploadRes.getUrl())
                            .isPrimary(true)
                            .product(product)
                            .build();
                    product.getImages().add(primaryImg);
                }
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    product.setMainImageUrl(textImage);
                }
            }

            Product updated = productRepository.save(product);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update product: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Upload / Update Product Image directly.
     */
    @PostMapping(value = {"/{id}/image", "/{id}/upload", "/{id}/upload-image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImageDirectly(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage
    ) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image, mainImage, productImage);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided", "success", false));
            }
            FileUploadResponse res = s3Service.uploadImage(targetFile, "products");
            Product product = pOpt.get();
            product.setMainImageUrl(res.getUrl());

            boolean updatedPrimary = false;
            for (ProductImage img : product.getImages()) {
                if (Boolean.TRUE.equals(img.getIsPrimary())) {
                    img.setImageUrl(res.getUrl());
                    updatedPrimary = true;
                    break;
                }
            }
            if (!updatedPrimary) {
                ProductImage primaryImg = ProductImage.builder()
                        .imageUrl(res.getUrl())
                        .isPrimary(true)
                        .product(product)
                        .build();
                product.getImages().add(primaryImg);
            }

            Product saved = productRepository.save(product);
            return ResponseEntity.ok(Map.of(
                    "message", "Product image uploaded successfully",
                    "product", saved,
                    "imageUrl", res.getUrl(),
                    "image", res.getUrl(),
                    "mainImageUrl", res.getUrl(),
                    "key", res.getKey(),
                    "presignedUrl", res.getPresignedUrl(),
                    "success", true
            ));
        } catch (Exception e) {
            log.error("Failed to upload product image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Product deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productRepository.findByTitleContainingIgnoreCase(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Product>> filterProducts(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(productRepository.findByCategoryId(categoryId));
        }
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Product>> getTrendingProducts() {
        return ResponseEntity.ok(productRepository.findByTrendingTrue());
    }

    // Product Images Endpoints (JSON)
    @PostMapping(value = "/{productId}/images", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductImage> addProductImageJson(@PathVariable Long productId, @RequestBody Map<String, Object> payload) {
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isPresent()) {
            Product product = pOpt.get();
            String imageUrl = (String) payload.getOrDefault("imageUrl", payload.get("image"));
            Boolean isPrimary = (Boolean) payload.getOrDefault("isPrimary", false);

            ProductImage img = ProductImage.builder()
                    .imageUrl(imageUrl)
                    .isPrimary(isPrimary)
                    .product(product)
                    .build();
            ProductImage saved = productImageRepository.save(img);

            if (Boolean.TRUE.equals(isPrimary)) {
                product.setMainImageUrl(imageUrl);
                productRepository.save(product);
            }

            return ResponseEntity.ok(saved);
        }
        return ResponseEntity.notFound().build();
    }

    // Product Images Endpoints (Multipart upload)
    @PostMapping(value = "/{productId}/images", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> addProductImageMultipart(
            @PathVariable Long productId,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "productImage", required = false) MultipartFile productImage,
            HttpServletRequest request
    ) {
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Product product = pOpt.get();
            MultipartFile targetFile = getFirstNonEmpty(file, image, productImage);
            String finalImageUrl = imageUrl;
            if (targetFile != null && !targetFile.isEmpty()) {
                FileUploadResponse res = s3Service.uploadImage(targetFile, "products");
                finalImageUrl = res.getUrl();
            } else if (!StringUtils.hasText(finalImageUrl)) {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    finalImageUrl = textImage;
                }
            }

            if (!StringUtils.hasText(finalImageUrl)) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file or URL provided", "success", false));
            }

            ProductImage img = ProductImage.builder()
                    .imageUrl(finalImageUrl)
                    .isPrimary(isPrimary)
                    .product(product)
                    .build();
            ProductImage saved = productImageRepository.save(img);

            if (Boolean.TRUE.equals(isPrimary) || product.getMainImageUrl() == null || product.getMainImageUrl().isEmpty()) {
                product.setMainImageUrl(finalImageUrl);
                productRepository.save(product);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to add product image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PutMapping("/images/{imageId}")
    public ResponseEntity<ProductImage> updateProductImage(@PathVariable Long imageId, @RequestBody Map<String, Object> payload) {
        Optional<ProductImage> imgOpt = productImageRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ProductImage img = imgOpt.get();
            if (payload.containsKey("imageUrl")) img.setImageUrl((String) payload.get("imageUrl"));
            if (payload.containsKey("image")) img.setImageUrl((String) payload.get("image"));
            if (payload.containsKey("isPrimary")) img.setIsPrimary((Boolean) payload.get("isPrimary"));
            ProductImage saved = productImageRepository.save(img);
            return ResponseEntity.ok(saved);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteProductImage(@PathVariable Long imageId) {
        if (productImageRepository.existsById(imageId)) {
            productImageRepository.deleteById(imageId);
            return ResponseEntity.ok(Map.of("message", "Product image deleted", "success", true));
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
}
