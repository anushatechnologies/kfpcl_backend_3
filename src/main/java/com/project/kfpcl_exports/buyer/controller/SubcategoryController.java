package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.model.Subcategory;
import com.project.kfpcl_exports.buyer.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("buyerSubcategoryController")
@RequestMapping("/api/buyer/subcategories")
@CrossOrigin(origins = "*")
public class SubcategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/{categoryId}")
    public ResponseEntity<List<Subcategory>> getSubcategoriesByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getSubcategoriesByCategoryId(categoryId));
    }
}
