package com.project.kfpcl_exports.admin.service;

import com.project.kfpcl_exports.admin.dto.ProductRequestDTO;
import com.project.kfpcl_exports.admin.dto.ProductResponseDTO;
import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.admin.model.ProductImage;
import com.project.kfpcl_exports.admin.model.Store;
import com.project.kfpcl_exports.admin.repository.CategoryRepository;
import com.project.kfpcl_exports.admin.repository.ProductRepository;
import com.project.kfpcl_exports.admin.repository.StoreRepository;
import com.project.kfpcl_exports.admin.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.project.kfpcl_exports.admin.dto.ProductVariantDTO;
import com.project.kfpcl_exports.admin.model.ProductVariant;
import com.project.kfpcl_exports.admin.repository.ProductVariantRepository;

@Service("adminProductService")
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponseDTO> getProductById(Long id) {
        return productRepository.findById(id).map(ProductResponseDTO::fromEntity);
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = new Product();
        mapDtoToEntity(request, product);

        // Associate Store
        associateStore(product, request.getStoreId(), request.getStoreName());

        // Setup primary image if needed
        if (StringUtils.hasText(product.getMainImageUrl())) {
            if (product.getImages() == null || product.getImages().isEmpty()) {
                ProductImage primaryImg = ProductImage.builder()
                        .imageUrl(product.getMainImageUrl())
                        .isPrimary(true)
                        .product(product)
                        .build();
                product.getImages().add(primaryImg);
            }
        }

        populateCategoryAndSubcategoryNames(product);

        Product saved = productRepository.save(product);
        return ProductResponseDTO.fromEntity(saved);
    }

    public Optional<ProductResponseDTO> updateProduct(Long id, ProductRequestDTO request) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty()) {
            return Optional.empty();
        }

        Product product = pOpt.get();
        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOriginalPrice() != null) product.setOriginalPrice(request.getOriginalPrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getUnit() != null) product.setUnit(request.getUnit());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getCategoryName() != null) product.setCategoryName(request.getCategoryName());
        if (request.getSubcategoryId() != null) product.setSubcategoryId(request.getSubcategoryId());
        if (request.getSubcategoryName() != null) product.setSubcategoryName(request.getSubcategoryName());
        if (request.getMainImageUrl() != null) product.setMainImageUrl(request.getMainImageUrl());
        if (request.getTrending() != null) product.setTrending(request.getTrending());
        if (request.getActive() != null) product.setActive(request.getActive());

        if (request.getVariants() != null) {
            List<com.project.kfpcl_exports.admin.model.ProductVariant> variantList = request.getVariants().stream().map(vDto -> {
                com.project.kfpcl_exports.admin.model.ProductVariant v = new com.project.kfpcl_exports.admin.model.ProductVariant();
                if (vDto.getId() != null) {
                    v.setId(vDto.getId());
                }
                v.setName(vDto.getName());
                v.setSku(vDto.getSku());
                v.setPrice(vDto.getPrice());
                v.setDiscountPrice(vDto.getDiscountPrice());
                v.setStock(vDto.getStock() != null ? vDto.getStock() : 0);
                v.setIsActive(vDto.getIsActive() != null ? vDto.getIsActive() : true);
                v.setDisplayOrder(vDto.getDisplayOrder() != null ? vDto.getDisplayOrder() : 1);
                v.setProduct(product);
                return v;
            }).collect(Collectors.toList());
            product.setVariants(variantList);
        }

        // Associate or update Store
        if (request.getStoreId() != null || StringUtils.hasText(request.getStoreName())) {
            associateStore(product, request.getStoreId(), request.getStoreName());
        } else if (request.getStoreId() == null && request.getStoreName() == null && Boolean.TRUE.equals(isStoreFieldExplicitlySet(request))) {
            product.setStore(null);
        }

        populateCategoryAndSubcategoryNames(product);

        Product updated = productRepository.save(product);
        return Optional.of(ProductResponseDTO.fromEntity(updated));
    }

    public void associateStore(Product product, Long storeId, String storeName) {
        if (storeId != null) {
            Optional<Store> storeOpt = storeRepository.findById(storeId);
            if (storeOpt.isPresent()) {
                Store store = storeOpt.get();
                product.setStore(store);
                product.setStoreId(store.getId());
                product.setStoreName(store.getName());
            } else {
                log.warn("Store with id {} not found", storeId);
                product.setStore(null);
                product.setStoreId(storeId);
                if (StringUtils.hasText(storeName)) {
                    product.setStoreName(storeName);
                }
            }
        } else if (StringUtils.hasText(storeName)) {
            Optional<Store> storeOpt = storeRepository.findByNameIgnoreCase(storeName.trim());
            if (storeOpt.isPresent()) {
                Store store = storeOpt.get();
                product.setStore(store);
                product.setStoreId(store.getId());
                product.setStoreName(store.getName());
            } else {
                product.setStore(null);
                product.setStoreName(storeName.trim());
            }
        } else {
            product.setStore(null);
        }
    }

    private void populateCategoryAndSubcategoryNames(Product product) {
        if (product.getCategoryId() != null && !StringUtils.hasText(product.getCategoryName())) {
            categoryRepository.findById(product.getCategoryId())
                    .ifPresent(category -> product.setCategoryName(category.getName()));
        }
        if (product.getSubcategoryId() != null && !StringUtils.hasText(product.getSubcategoryName())) {
            subcategoryRepository.findById(product.getSubcategoryId())
                    .ifPresent(subcategory -> product.setSubcategoryName(subcategory.getName()));
        }
    }

    private void mapDtoToEntity(ProductRequestDTO request, Product product) {
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStock(request.getStock());
        product.setUnit(request.getUnit());
        product.setCategoryId(request.getCategoryId());
        product.setCategoryName(request.getCategoryName());
        product.setSubcategoryId(request.getSubcategoryId());
        product.setSubcategoryName(request.getSubcategoryName());
        product.setMainImageUrl(request.getMainImageUrl());
        product.setRating(request.getRating());
        product.setReviewCount(request.getReviewCount());
        product.setTrending(request.getTrending());
        product.setActive(request.getActive());

        if (request.getVariants() != null) {
            List<com.project.kfpcl_exports.admin.model.ProductVariant> variantList = request.getVariants().stream().map(vDto -> {
                com.project.kfpcl_exports.admin.model.ProductVariant v = new com.project.kfpcl_exports.admin.model.ProductVariant();
                if (vDto.getId() != null) {
                    v.setId(vDto.getId());
                }
                v.setName(vDto.getName());
                v.setSku(vDto.getSku());
                v.setPrice(vDto.getPrice());
                v.setDiscountPrice(vDto.getDiscountPrice());
                v.setStock(vDto.getStock() != null ? vDto.getStock() : 0);
                v.setIsActive(vDto.getIsActive() != null ? vDto.getIsActive() : true);
                v.setDisplayOrder(vDto.getDisplayOrder() != null ? vDto.getDisplayOrder() : 1);
                v.setProduct(product);
                return v;
            }).collect(Collectors.toList());
            product.setVariants(variantList);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDTO> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductIdOrderByDisplayOrderAscIdAsc(productId).stream()
                .map(v -> ProductVariantDTO.builder()
                        .id(v.getId())
                        .name(v.getName())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .discountPrice(v.getDiscountPrice())
                        .stock(v.getStock())
                        .isActive(v.getIsActive())
                        .displayOrder(v.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }

    public ProductVariantDTO addVariantToProduct(Long productId, ProductVariantDTO dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        ProductVariant variant = ProductVariant.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .price(dto.getPrice() != null ? dto.getPrice() : 0.0)
                .discountPrice(dto.getDiscountPrice())
                .stock(dto.getStock() != null ? dto.getStock() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 1)
                .product(product)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);
        return ProductVariantDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .sku(saved.getSku())
                .price(saved.getPrice())
                .discountPrice(saved.getDiscountPrice())
                .stock(saved.getStock())
                .isActive(saved.getIsActive())
                .displayOrder(saved.getDisplayOrder())
                .build();
    }

    public Optional<ProductVariantDTO> updateProductVariant(Long productId, Long variantId, ProductVariantDTO dto) {
        Optional<ProductVariant> vOpt = productVariantRepository.findById(variantId);
        if (vOpt.isEmpty()) {
            return Optional.empty();
        }
        ProductVariant variant = vOpt.get();
        if (dto.getName() != null) variant.setName(dto.getName());
        if (dto.getSku() != null) variant.setSku(dto.getSku());
        if (dto.getPrice() != null) variant.setPrice(dto.getPrice());
        if (dto.getDiscountPrice() != null) variant.setDiscountPrice(dto.getDiscountPrice());
        if (dto.getStock() != null) variant.setStock(dto.getStock());
        if (dto.getIsActive() != null) variant.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) variant.setDisplayOrder(dto.getDisplayOrder());

        ProductVariant updated = productVariantRepository.save(variant);
        return Optional.of(ProductVariantDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .sku(updated.getSku())
                .price(updated.getPrice())
                .discountPrice(updated.getDiscountPrice())
                .stock(updated.getStock())
                .isActive(updated.getIsActive())
                .displayOrder(updated.getDisplayOrder())
                .build());
    }

    public boolean deleteProductVariant(Long productId, Long variantId) {
        Optional<ProductVariant> vOpt = productVariantRepository.findById(variantId);
        if (vOpt.isPresent()) {
            productVariantRepository.deleteById(variantId);
            return true;
        }
        return false;
    }

    private Boolean isStoreFieldExplicitlySet(ProductRequestDTO request) {
        return false;
    }
}
