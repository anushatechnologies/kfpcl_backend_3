package com.project.kfpcl_exports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String key;
    private String url;
    private String presignedUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}
