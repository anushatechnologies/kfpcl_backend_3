package com.project.kfpcl_exports.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.admin.model.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Double price;
    private Double originalPrice;
    private Integer stock;
    private String unit;

    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;

    private Long storeId;
    private String storeName;
    private StoreSummaryDTO store;

    private String mainImageUrl;
    private Double rating;
    private Integer reviewCount;
    private Boolean trending;
    private Boolean active;

    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    private LocalDateTime createdAt;

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return mainImageUrl;
    }

    @JsonProperty("image")
    public String getImage() {
        return mainImageUrl;
    }

    @JsonProperty("name")
    public String getName() {
        return title;
    }

    public static ProductResponseDTO fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        Long sId = product.getStore() != null ? product.getStore().getId() : product.getStoreId();
        String sName = product.getStore() != null ? product.getStore().getName() : product.getStoreName();

        StoreSummaryDTO sSummary = null;
        if (product.getStore() != null) {
            sSummary = StoreSummaryDTO.builder()
                    .id(product.getStore().getId())
                    .name(product.getStore().getName())
                    .city(product.getStore().getCity())
                    .state(product.getStore().getState())
                    .country(product.getStore().getCountry())
                    .build();
        } else if (sId != null || sName != null) {
            sSummary = StoreSummaryDTO.builder()
                    .id(sId)
                    .name(sName)
                    .build();
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .unit(product.getUnit())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .subcategoryId(product.getSubcategoryId())
                .subcategoryName(product.getSubcategoryName())
                .storeId(sId)
                .storeName(sName)
                .store(sSummary)
                .mainImageUrl(product.getMainImageUrl())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .trending(product.getTrending())
                .active(product.getActive())
                .images(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
