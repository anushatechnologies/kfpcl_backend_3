package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.model.Subcategory;
import com.project.kfpcl_exports.buyer.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("buyerSubcategoryController")
@RequestMapping({"/api/buyer/subcategories", "/api/buyer/sub-categories"})
public class SubcategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Subcategory>> getAllSubcategories(
            @RequestParam(name = "categoryId", required = false) Long categoryId
    ) {
        if (categoryId != null) {
            return ResponseEntity.ok(categoryService.getSubcategoriesByCategoryId(categoryId));
        }
        return ResponseEntity.ok(categoryService.getAllSubcategories());
    }

    @GetMapping({
            "/{categoryId}",
            "/category/{categoryId}"
    })
    public ResponseEntity<List<Subcategory>> getSubcategoriesByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getSubcategoriesByCategoryId(categoryId));
    }
}
