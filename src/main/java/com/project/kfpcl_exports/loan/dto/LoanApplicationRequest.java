package com.project.kfpcl_exports.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationRequest {

    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String email;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum loan amount is 1,000")
    private Double loanAmount;

    @NotBlank(message = "Loan purpose is required")
    private String loanPurpose;

    private Double annualIncome;

    private Double landSizeAcres;

    private String cropType;

    private String farmLocation;
}
