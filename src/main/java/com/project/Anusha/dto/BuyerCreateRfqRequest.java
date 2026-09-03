package com.project.Anusha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerCreateRfqRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotBlank(message = "quantity is required")
    private String quantity;

    @NotBlank(message = "deliveryLocation is required")
    private String deliveryLocation;

    private String buyerMessage;
}
