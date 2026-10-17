package com.project.kfpcl_exports.loan.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanDocumentDto {
    private Long id;
    private Long applicationId;
    private String documentCategory;
    private String originalFilename;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadTimestamp;
    private String uploadStatus;
    private String downloadUrl;
    private String presignedUrl;
}
