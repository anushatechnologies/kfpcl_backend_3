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

    @JsonAlias({"name", "variantName", "variant_name", "variantValue", "variant_value", "value", "variant"})
    private String name;

    @JsonAlias({"unit", "variantUnit", "variant_unit"})
    private String unit;

    @JsonProperty("variantName")
    public String getVariantName() {
        return getEffectiveName();
    }

    @JsonProperty("value")
    public String getValue() {
        return name;
    }

    @JsonProperty("unit")
    public String getUnit() {
        return unit;
    }

    public String getEffectiveName() {
        if (name != null && !name.isBlank()) {
            if (unit != null && !unit.isBlank() && !name.toLowerCase().contains(unit.toLowerCase())) {
                return name.trim() + " " + unit.trim();
            }
            return name.trim();
        }
        if (unit != null && !unit.isBlank()) {
            return unit.trim();
        }
        return "";
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
