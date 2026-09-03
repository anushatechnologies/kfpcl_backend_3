package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.model.Subcategory;
import com.project.kfpcl_exports.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryRepository subcategoryRepository;

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

    @PostMapping
    public ResponseEntity<Subcategory> createSubcategory(@RequestBody Subcategory subcategory) {
        Subcategory saved = subcategoryRepository.save(subcategory);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subcategory> updateSubcategory(@PathVariable Long id, @RequestBody Subcategory subcategoryDetails) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isPresent()) {
            Subcategory sub = subOpt.get();
            sub.setName(subcategoryDetails.getName());
            sub.setDescription(subcategoryDetails.getDescription());
            sub.setCategoryId(subcategoryDetails.getCategoryId());
            sub.setCategoryName(subcategoryDetails.getCategoryName());
            if (subcategoryDetails.getImageUrl() != null) sub.setImageUrl(subcategoryDetails.getImageUrl());
            if (subcategoryDetails.getActive() != null) sub.setActive(subcategoryDetails.getActive());
            Subcategory updated = subcategoryRepository.save(sub);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteSubcategory(@PathVariable Long id) {
        Optional<Subcategory> subOpt = subcategoryRepository.findById(id);
        if (subOpt.isPresent()) {
            Subcategory sub = subOpt.get();
            sub.setDeleted(true);
            subcategoryRepository.save(sub);
            return ResponseEntity.ok(Map.of("message", "Subcategory soft deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Map<String, Object>> hardDeleteSubcategory(@PathVariable Long id) {
        if (subcategoryRepository.existsById(id)) {
            subcategoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Subcategory permanently deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }
}
