package com.project.kfpcl_exports.buyer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyerLoginRequest {

    @NotBlank(message = "Phone number or email is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
