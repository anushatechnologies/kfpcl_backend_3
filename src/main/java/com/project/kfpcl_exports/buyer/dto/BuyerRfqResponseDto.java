package com.project.kfpcl_exports.buyer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuyerRfqResponseDto {

    private Long id;
    private String rfqId;
    private String rfqCode;
    private ProductSummaryDto product;
    private Long productId;
    private String title;
    private String productName;
    private String buyerName;
    private String buyerPhone;
    private String quantity;
    private String unit;
    private String deliveryLocation;
    private String subject;
    private String buyerMessage;
    private String fileUrl;
    private RfqStatus status;
    private Long parentRfqId;
    private String parentRfqCode;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RfqResponseSummaryDto response;
    private boolean contactAvailable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummaryDto {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqResponseSummaryDto {
        private String quoteId;
        private Double quotedPrice;
        private Double offeredPrice;
        private Double totalAmount;
        private String availableQuantity;
        private String deliveryTime;
        private String leadTime;
        private String notes;
        private String responseMessage;
        private String status;
        private LocalDateTime createdAt;
    }
}
