package com.project.kfpcl_exports.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSummaryDTO {
    private Long id;
    private String name;
    private String city;
    private String state;
    private String country;
}
