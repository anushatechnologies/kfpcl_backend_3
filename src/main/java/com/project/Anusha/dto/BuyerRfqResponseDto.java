package com.project.Anusha.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.Anusha.enums.RfqStatus;
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
    private String quantity;
    private String deliveryLocation;
    private String buyerMessage;
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
        private Double quotedPrice;
        private String availableQuantity;
        private String deliveryTime;
        private String responseMessage;
        private LocalDateTime createdAt;
    }
}
