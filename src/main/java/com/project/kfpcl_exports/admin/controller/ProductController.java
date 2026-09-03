package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.admin.model.ProductImage;
import com.project.kfpcl_exports.admin.repository.ProductImageRepository;
import com.project.kfpcl_exports.admin.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("adminProductController")
@RequestMapping({"/api/products", "/api/admin/products"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

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

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
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

    // Product Images Endpoints
    @PostMapping("/{productId}/images")
    public ResponseEntity<ProductImage> addProductImage(@PathVariable Long productId, @RequestBody Map<String, Object> payload) {
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isPresent()) {
            Product product = pOpt.get();
            String imageUrl = (String) payload.get("imageUrl");
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

    @PutMapping("/images/{imageId}")
    public ResponseEntity<ProductImage> updateProductImage(@PathVariable Long imageId, @RequestBody Map<String, Object> payload) {
        Optional<ProductImage> imgOpt = productImageRepository.findById(imageId);
        if (imgOpt.isPresent()) {
            ProductImage img = imgOpt.get();
            if (payload.containsKey("imageUrl")) img.setImageUrl((String) payload.get("imageUrl"));
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
}
