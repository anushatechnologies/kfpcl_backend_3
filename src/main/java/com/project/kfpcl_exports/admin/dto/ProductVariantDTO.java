package com.project.kfpcl_exports.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductVariantDTO {

    private Long id;

    @JsonAlias({"name", "variantName", "variant_name"})
    private String name;

    @JsonProperty("variantName")
    public String getVariantName() {
        return name;
    }

    private String sku;

    private Double price;

    @JsonAlias({"discountPrice", "discount_price"})
    private Double discountPrice;

    @JsonAlias({"stock", "stockQuantity", "stock_quantity"})
    private Integer stock;

    @JsonProperty("stockQuantity")
    public Integer getStockQuantity() {
        return stock;
    }

    @JsonAlias({"active", "isActive", "is_active"})
    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonAlias({"displayOrder", "display_order"})
    private Integer displayOrder;
}
