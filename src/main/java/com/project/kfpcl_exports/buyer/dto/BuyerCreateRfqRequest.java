package com.project.kfpcl_exports.buyer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerCreateRfqRequest {

    @JsonAlias({"product_id", "item_id", "itemId"})
    private Long productId;

    @JsonAlias({"product_name", "productName", "productTitle", "product_title", "commodity", "item", "product"})
    private String productName;

    @JsonAlias({"name", "contactName"})
    private String buyerName;

    @JsonAlias({"phone", "phoneNumber", "mobile", "buyerMobile"})
    private String buyerPhone;

    @NotBlank(message = "quantity is required")
    private String quantity;

    @NotBlank(message = "deliveryLocation is required")
    private String deliveryLocation;

    private String subject;

    @JsonAlias({"message", "notes", "description"})
    private String buyerMessage;

    private String email;
    private String userEmail;
    private String phone;
    private String phoneNumber;
    private String fileUrl;
}
