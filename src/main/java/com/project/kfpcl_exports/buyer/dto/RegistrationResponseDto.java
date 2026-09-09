package com.project.kfpcl_exports.buyer.dto;

import com.project.kfpcl_exports.buyer.enums.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class RegistrationResponseDto {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String companyName;
    private BusinessType businessType;
    private String state;
    private String city;
    private String gstin;
    private String gstinPhotoUrl;
    private String panNumber;
    private String panCardUrl;
    private String status;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime createdAt;
}
