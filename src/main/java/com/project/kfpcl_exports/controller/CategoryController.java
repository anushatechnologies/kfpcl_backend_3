package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.model.Category;
import com.project.kfpcl_exports.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

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

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isPresent()) {
            Category category = catOpt.get();
            category.setName(categoryDetails.getName());
            category.setDescription(categoryDetails.getDescription());
            category.setImageUrl(categoryDetails.getImageUrl());
            if (categoryDetails.getDiscount() != null) category.setDiscount(categoryDetails.getDiscount());
            if (categoryDetails.getActive() != null) category.setActive(categoryDetails.getActive());
            Category updated = categoryRepository.save(category);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
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
}
