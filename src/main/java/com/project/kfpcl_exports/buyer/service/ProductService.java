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

    @Autowired
    private com.project.kfpcl_exports.admin.repository.ProductVariantRepository productVariantRepository;

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

        List<java.util.Map<String, Object>> mappedVariants = new java.util.ArrayList<>();
        List<com.project.kfpcl_exports.admin.model.ProductVariant> variants = ap.getVariants();
        if ((variants == null || variants.isEmpty()) && ap.getId() != null && productVariantRepository != null) {
            variants = productVariantRepository.findByProductIdAndIsActiveTrue(ap.getId());
        }

        if (variants != null && !variants.isEmpty()) {
            for (com.project.kfpcl_exports.admin.model.ProductVariant v : variants) {
                if (v.getIsActive() == null || v.getIsActive()) {
                    java.util.Map<String, Object> varMap = new java.util.LinkedHashMap<>();
                    varMap.put("id", v.getId());
                    varMap.put("name", v.getName());
                    varMap.put("variantName", v.getName());
                    varMap.put("value", v.getName());
                    varMap.put("unit", ap.getUnit());
                    varMap.put("sku", v.getSku());
                    varMap.put("price", v.getPrice() != null ? v.getPrice() : 0.0);
                    varMap.put("discountPrice", v.getDiscountPrice());
                    varMap.put("mrp", v.getPrice() != null ? v.getPrice() : 0.0);
                    varMap.put("stockQuantity", v.getStock() != null ? v.getStock() : 0);
                    varMap.put("stock", v.getStock() != null ? v.getStock() : 0);
                    varMap.put("isActive", v.getIsActive() != null ? v.getIsActive() : true);
                    varMap.put("displayOrder", v.getDisplayOrder() != null ? v.getDisplayOrder() : 1);
                    mappedVariants.add(varMap);
                }
            }
        }

        Product buyerProd = Product.builder()
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

        buyerProd.setVariants(mappedVariants);
        return buyerProd;
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

        Page<Product> buyerPage = productRepository.filterProducts(categoryId, subcategoryId, minPrice, maxPrice, query, pageable);
        if (buyerPage != null && buyerPage.getContent() != null) {
            buyerPage.getContent().forEach(this::populateVariantsForBuyerProduct);
        }
        return buyerPage;
    }

    public Optional<Product> getProductById(Long id) {
        Optional<com.project.kfpcl_exports.admin.model.Product> adminOpt = adminProductRepository.findById(id);
        if (adminOpt.isPresent()) {
            return adminOpt.map(this::mapAdminToBuyerProduct);
        }
        Optional<Product> buyerOpt = productRepository.findById(id).filter(p -> p.getIsActive() != null && p.getIsActive());
        buyerOpt.ifPresent(this::populateVariantsForBuyerProduct);
        return buyerOpt;
    }

    private void populateVariantsForBuyerProduct(Product p) {
        if (p == null || p.getId() == null) return;
        List<java.util.Map<String, Object>> mappedVariants = new java.util.ArrayList<>();
        if (productVariantRepository != null) {
            List<com.project.kfpcl_exports.admin.model.ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(p.getId());
            if (variants != null && !variants.isEmpty()) {
                for (com.project.kfpcl_exports.admin.model.ProductVariant v : variants) {
                    if (v.getIsActive() == null || v.getIsActive()) {
                        java.util.Map<String, Object> varMap = new java.util.LinkedHashMap<>();
                        varMap.put("id", v.getId());
                        varMap.put("name", v.getName());
                        varMap.put("variantName", v.getName());
                        varMap.put("value", v.getName());
                        varMap.put("unit", p.getUnit());
                        varMap.put("sku", v.getSku());
                        varMap.put("price", v.getPrice() != null ? v.getPrice() : 0.0);
                        varMap.put("discountPrice", v.getDiscountPrice());
                        varMap.put("mrp", v.getPrice() != null ? v.getPrice() : 0.0);
                        varMap.put("stockQuantity", v.getStock() != null ? v.getStock() : 0);
                        varMap.put("stock", v.getStock() != null ? v.getStock() : 0);
                        varMap.put("isActive", v.getIsActive() != null ? v.getIsActive() : true);
                        varMap.put("displayOrder", v.getDisplayOrder() != null ? v.getDisplayOrder() : 1);
                        mappedVariants.add(varMap);
                    }
                }
            }
        }
        p.setVariants(mappedVariants);
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
        List<Product> buyerResults = productRepository.fullTextSearch(query.trim());
        buyerResults.forEach(this::populateVariantsForBuyerProduct);
        return buyerResults;
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
        list.forEach(this::populateVariantsForBuyerProduct);
        return list;
    }

    public List<Product> getBestsellers() {
        List<com.project.kfpcl_exports.admin.model.Product> adminAll = adminProductRepository.findAll();
        if (!adminAll.isEmpty()) {
            return adminAll.stream().map(this::mapAdminToBuyerProduct).collect(Collectors.toList());
        }
        List<Product> list = productRepository.findBestsellers(PageRequest.of(0, 10));
        list.forEach(this::populateVariantsForBuyerProduct);
        return list;
    }
}

