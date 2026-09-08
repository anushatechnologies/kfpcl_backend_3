package com.project.kfpcl_exports.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuotationRequest {
    private Double unitPrice;
    private Integer quantity;
    private String deliveryDays;
    private String notes;
    private Object moq;
    private String availability;
    private String paymentTerms;
    private Double targetPrice;
    private String comments;
}
