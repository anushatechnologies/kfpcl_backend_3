package com.project.kfpcl_exports.dto;

import lombok.Data;

@Data
public class QuotationRequest {
    private Double unitPrice;
    private Integer quantity;
    private String deliveryDays;
    private String notes;
}
