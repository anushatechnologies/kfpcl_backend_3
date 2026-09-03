package com.project.kfpcl_exports.buyer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponseDto {

    private String contactName;
    private String contactPhone;
    private String contactEmail;
}
