package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.CategoryDetailResponse;
import com.project.kfpcl_exports.buyer.model.Category;
import com.project.kfpcl_exports.buyer.model.Subcategory;
import com.project.kfpcl_exports.buyer.repository.CategoryRepository;
import com.project.kfpcl_exports.buyer.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Autowired(required = false)
    @Qualifier("adminCategoryRepository")
    private com.project.kfpcl_exports.admin.repository.CategoryRepository adminCategoryRepository;

    @Autowired(required = false)
    @Qualifier("adminSubcategoryRepository")
    private com.project.kfpcl_exports.admin.repository.SubcategoryRepository adminSubcategoryRepository;

    private Category mapAdminToBuyerCategory(com.project.kfpcl_exports.admin.model.Category ac) {
        if (ac == null) return null;
        String img = ac.getImageUrl() != null && !ac.getImageUrl().isBlank() ? ac.getImageUrl() : ac.getImage();
        Integer sort = ac.getDisplayOrder() != null ? ac.getDisplayOrder() : (ac.getOrder() != null ? ac.getOrder() : 1);
        boolean active = (ac.getActive() == null || ac.getActive()) && !Boolean.TRUE.equals(ac.getDeleted());
        return Category.builder()
                .id(ac.getId())
                .name(ac.getName())
                .description(ac.getDescription())
                .imageUrl(img)
                .sortOrder(sort)
                .isActive(active)
                .createdAt(ac.getCreatedAt())
                .build();
    }

    private Subcategory mapAdminToBuyerSubcategory(com.project.kfpcl_exports.admin.model.Subcategory as) {
        if (as == null) return null;
        Subcategory sub = new Subcategory();
        sub.setId(as.getId());
        sub.setName(as.getName());
        sub.setImageUrl(as.getImageUrl() != null && !as.getImageUrl().isBlank() ? as.getImageUrl() : as.getImage());
        sub.setCreatedAt(as.getCreatedAt());
        if (as.getCategoryId() != null) {
            Category cat = new Category();
            cat.setId(as.getCategoryId());
            cat.setName(as.getCategoryName());
            sub.setCategory(cat);
        }
        return sub;
    }

    public List<Category> getAllActiveCategories() {
        List<Category> buyerCategories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        if (buyerCategories != null && !buyerCategories.isEmpty()) {
            return buyerCategories;
        }
        if (adminCategoryRepository != null) {
            return adminCategoryRepository.findByDeletedFalse().stream()
                    .filter(c -> c.getActive() == null || c.getActive())
                    .map(this::mapAdminToBuyerCategory)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public Optional<CategoryDetailResponse> getCategoryDetails(Long id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent() && (categoryOpt.get().getIsActive() == null || categoryOpt.get().getIsActive())) {
            long count = subcategoryRepository.countByCategoryId(id);
            return Optional.of(new CategoryDetailResponse(categoryOpt.get(), count));
        }

        if (adminCategoryRepository != null) {
            Optional<com.project.kfpcl_exports.admin.model.Category> adminCatOpt = adminCategoryRepository.findById(id);
            if (adminCatOpt.isPresent()) {
                com.project.kfpcl_exports.admin.model.Category ac = adminCatOpt.get();
                if (!Boolean.TRUE.equals(ac.getDeleted())) {
                    long count = 0;
                    if (adminSubcategoryRepository != null) {
                        count = adminSubcategoryRepository.findByCategoryIdAndDeletedFalse(id).size();
                    }
                    Category mapped = mapAdminToBuyerCategory(ac);
                    return Optional.of(new CategoryDetailResponse(mapped, count));
                }
            }
        }
        return Optional.empty();
    }

    public List<Subcategory> getSubcategoriesByCategoryId(Long categoryId) {
        List<Subcategory> buyerSubs = subcategoryRepository.findByCategoryId(categoryId);
        if (buyerSubs != null && !buyerSubs.isEmpty()) {
            return buyerSubs;
        }
        if (adminSubcategoryRepository != null) {
            return adminSubcategoryRepository.findByCategoryIdAndDeletedFalse(categoryId).stream()
                    .filter(s -> s.getActive() == null || s.getActive())
                    .map(this::mapAdminToBuyerSubcategory)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public List<Subcategory> getAllSubcategories() {
        List<Subcategory> buyerSubs = subcategoryRepository.findAll();
        if (buyerSubs != null && !buyerSubs.isEmpty()) {
            return buyerSubs;
        }
        if (adminSubcategoryRepository != null) {
            return adminSubcategoryRepository.findByDeletedFalse().stream()
                    .filter(s -> s.getActive() == null || s.getActive())
                    .map(this::mapAdminToBuyerSubcategory)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
