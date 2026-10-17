package com.project.kfpcl_exports.loan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLoanStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
    private String rejectionReason;
    private String adminNotes;
}
