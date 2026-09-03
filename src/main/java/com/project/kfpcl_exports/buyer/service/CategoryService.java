package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.CategoryDetailResponse;
import com.project.kfpcl_exports.buyer.model.Category;
import com.project.kfpcl_exports.buyer.model.Subcategory;
import com.project.kfpcl_exports.buyer.repository.CategoryRepository;
import com.project.kfpcl_exports.buyer.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    public Optional<CategoryDetailResponse> getCategoryDetails(Long id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent() && Boolean.TRUE.equals(categoryOpt.get().getIsActive())) {
            long count = subcategoryRepository.countByCategoryId(id);
            return Optional.of(new CategoryDetailResponse(categoryOpt.get(), count));
        }
        return Optional.empty();
    }

    public List<Subcategory> getSubcategoriesByCategoryId(Long categoryId) {
        return subcategoryRepository.findByCategoryId(categoryId);
    }
}
