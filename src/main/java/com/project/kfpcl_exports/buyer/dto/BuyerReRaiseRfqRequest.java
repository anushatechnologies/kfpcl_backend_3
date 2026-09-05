package com.project.kfpcl_exports.buyer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerReRaiseRfqRequest {

    @NotBlank(message = "quantity is required")
    private String quantity;

    @NotBlank(message = "deliveryLocation is required")
    private String deliveryLocation;

    private String buyerMessage;
}
