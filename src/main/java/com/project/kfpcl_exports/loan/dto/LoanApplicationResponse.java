package com.project.kfpcl_exports.loan.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationResponse {
    private Long id;
    private String applicationNumber;
    private Long userId;
    private String customerId;
    private String applicantName;
    private String phoneNumber;
    private String email;
    private Double loanAmount;
    private String loanPurpose;
    private Double annualIncome;
    private Double landSizeAcres;
    private String cropType;
    private String farmLocation;
    private String status;
    private String rejectionReason;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<LoanDocumentDto> documents = new ArrayList<>();
}
