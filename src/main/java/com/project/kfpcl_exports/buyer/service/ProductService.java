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
        Double price = ap.getPrice() != null ? ap.getPrice() : 0.0;
        Double originalPrice = (ap.getOriginalPrice() != null && ap.getOriginalPrice() > 0) ? ap.getOriginalPrice() : price;

        return Product.builder()
                .id(ap.getId())
                .name(ap.getTitle())
                .description(ap.getDescription())
                .mainImageUrl(ap.getMainImageUrl())
                .imageUrl(ap.getMainImageUrl())
                .indicativePrice(String.format("%.2f", price))
                .numericPrice(BigDecimal.valueOf(price))
                .originalPrice(originalPrice)
                .stockQuantity(ap.getStock() != null ? ap.getStock() : 100)
                .unit(ap.getUnit())
                .storeId(ap.getStoreId())
                .storeName(ap.getStoreName())
                .isActive(ap.getActive() != null ? ap.getActive() : true)
                .category(cat)
                .subcategory(subcat)
                .createdAt(ap.getCreatedAt())
                .build();
    }

    public Page<Product> getFilteredProducts(Long categoryId, Long subcategoryId, int page, int limit,
                                            BigDecimal minPrice, BigDecimal maxPrice, String query) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit > 0 ? limit : 10);
        List<com.project.kfpcl_exports.admin.model.Product> adminList = adminProductRepository.findAll();
        if (!adminList.isEmpty()) {
            List<Product> adminMapped = adminList.stream()
                    .filter(ap -> (ap.getActive() == null || ap.getActive()))
                    .filter(ap -> categoryId == null || categoryId.equals(ap.getCategoryId()))
                    .filter(ap -> subcategoryId == null || subcategoryId.equals(ap.getSubcategoryId()))
                    .filter(ap -> minPrice == null || (ap.getPrice() != null && BigDecimal.valueOf(ap.getPrice()).compareTo(minPrice) >= 0))
                    .filter(ap -> maxPrice == null || (ap.getPrice() != null && BigDecimal.valueOf(ap.getPrice()).compareTo(maxPrice) <= 0))
                    .filter(ap -> query == null || query.trim().isEmpty() || (ap.getTitle() != null && ap.getTitle().toLowerCase().contains(query.toLowerCase())))
                    .map(this::mapAdminToBuyerProduct)
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), adminMapped.size());
            List<Product> pageContent = (start <= end && start < adminMapped.size())
                    ? adminMapped.subList(start, end)
                    : java.util.Collections.emptyList();
            return new PageImpl<>(pageContent, pageable, adminMapped.size());
        }

        return productRepository.filterProducts(categoryId, subcategoryId, minPrice, maxPrice, query, pageable);
    }

    public Optional<Product> getProductById(Long id) {
        Optional<com.project.kfpcl_exports.admin.model.Product> adminOpt = adminProductRepository.findById(id);
        if (adminOpt.isPresent()) {
            return adminOpt.map(this::mapAdminToBuyerProduct);
        }
        return productRepository.findById(id).filter(p -> p.getIsActive() != null && p.getIsActive());
    }

    public List<String> getSearchSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }
        List<String> adminSuggestions = adminProductRepository.findByTitleContainingIgnoreCase(prefix.trim())
                .stream()
                .map(com.project.kfpcl_exports.admin.model.Product::getTitle)
                .distinct()
                .collect(Collectors.toList());
        if (!adminSuggestions.isEmpty()) {
            return adminSuggestions;
        }
        return productRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrue(prefix.trim())
                .stream()
                .map(Product::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<com.project.kfpcl_exports.admin.model.Product> adminResults = adminProductRepository.findByTitleContainingIgnoreCase(query.trim());
        if (!adminResults.isEmpty()) {
            return adminResults.stream().map(this::mapAdminToBuyerProduct).collect(Collectors.toList());
        }
        return productRepository.fullTextSearch(query.trim());
    }

    public List<Product> getTrendingProducts() {
        List<com.project.kfpcl_exports.admin.model.Product> adminTrending = adminProductRepository.findByTrendingTrue();
        if (!adminTrending.isEmpty()) {
            return adminTrending.stream().map(this::mapAdminToBuyerProduct).collect(Collectors.toList());
        }
        List<Product> list = productRepository.findTrendingProducts(PageRequest.of(0, 10));
        if (list.isEmpty()) {
            return adminProductRepository.findAll().stream().map(this::mapAdminToBuyerProduct).collect(Collectors.toList());
        }
        return list;
    }

    public List<Product> getBestsellers() {
        List<com.project.kfpcl_exports.admin.model.Product> adminAll = adminProductRepository.findAll();
        if (!adminAll.isEmpty()) {
            return adminAll.stream().map(this::mapAdminToBuyerProduct).collect(Collectors.toList());
        }
        return productRepository.findBestsellers(PageRequest.of(0, 10));
    }
}

