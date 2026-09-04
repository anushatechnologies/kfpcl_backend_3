package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.model.Category;
import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.model.Subcategory;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository;

    private Product mapAdminToBuyerProduct(com.project.kfpcl_exports.admin.model.Product ap) {
        if (ap == null) return null;
        Category cat = null;
        if (ap.getCategoryId() != null) {
            cat = Category.builder().id(ap.getCategoryId()).name(ap.getCategoryName()).build();
        }
        Subcategory subcat = null;
        if (ap.getSubcategoryId() != null) {
            subcat = new Subcategory();
            subcat.setId(ap.getSubcategoryId());
            subcat.setName(ap.getSubcategoryName());
        }
        return Product.builder()
                .id(ap.getId())
                .name(ap.getTitle())
                .description(ap.getDescription())
                .mainImageUrl(ap.getMainImageUrl())
                .imageUrl(ap.getMainImageUrl())
                .indicativePrice(ap.getPrice() != null ? String.valueOf(ap.getPrice()) : null)
                .numericPrice(ap.getPrice() != null ? BigDecimal.valueOf(ap.getPrice()) : null)
                .isActive(ap.getActive() != null ? ap.getActive() : true)
                .category(cat)
                .subcategory(subcat)
                .createdAt(ap.getCreatedAt())
                .build();
    }

    public Page<Product> getFilteredProducts(Long categoryId, Long subcategoryId, int page, int limit,
                                            BigDecimal minPrice, BigDecimal maxPrice, String query) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit > 0 ? limit : 10);
        Page<Product> buyerPage = productRepository.filterProducts(categoryId, subcategoryId, minPrice, maxPrice, query, pageable);
        if (buyerPage.hasContent()) {
            return buyerPage;
        }
        List<Product> adminMapped = adminProductRepository.findAll().stream()
                .filter(ap -> (ap.getActive() == null || ap.getActive()))
                .filter(ap -> categoryId == null || categoryId.equals(ap.getCategoryId()))
                .filter(ap -> subcategoryId == null || subcategoryId.equals(ap.getSubcategoryId()))
                .filter(ap -> query == null || query.trim().isEmpty() || (ap.getTitle() != null && ap.getTitle().toLowerCase().contains(query.toLowerCase())))
                .map(this::mapAdminToBuyerProduct)
                .collect(Collectors.toList());
        return new PageImpl<>(adminMapped, pageable, adminMapped.size());
    }

    public Optional<Product> getProductById(Long id) {
        Optional<Product> buyerOpt = productRepository.findById(id).filter(p -> p.getIsActive() != null && p.getIsActive());
        if (buyerOpt.isPresent()) {
            return buyerOpt;
        }
        return adminProductRepository.findById(id).map(this::mapAdminToBuyerProduct);
    }

    public List<String> getSearchSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }
        List<String> list = productRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrue(prefix.trim())
                .stream()
                .map(Product::getName)
                .distinct()
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            return adminProductRepository.findByTitleContainingIgnoreCase(prefix.trim())
                    .stream()
                    .map(com.project.kfpcl_exports.admin.model.Product::getTitle)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<Product> list = productRepository.fullTextSearch(query.trim());
        if (list.isEmpty()) {
            return adminProductRepository.findByTitleContainingIgnoreCase(query.trim())
                    .stream()
                    .map(this::mapAdminToBuyerProduct)
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<Product> getTrendingProducts() {
        List<Product> list = productRepository.findTrendingProducts(PageRequest.of(0, 10));
        if (list.isEmpty()) {
            return adminProductRepository.findByTrendingTrue()
                    .stream()
                    .map(this::mapAdminToBuyerProduct)
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<Product> getBestsellers() {
        List<Product> list = productRepository.findBestsellers(PageRequest.of(0, 10));
        if (list.isEmpty()) {
            return adminProductRepository.findAll()
                    .stream()
                    .map(this::mapAdminToBuyerProduct)
                    .collect(Collectors.toList());
        }
        return list;
    }
}

