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

    private String supplierName;
    private String contactPerson;
    private String contactName;
    private String phone;
    private String contactPhone;
    private String email;
    private String contactEmail;
    private String dispatchWarehouse;
}
