package com.project.kfpcl_exports.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String title;
    private String message;
    private String recipientType;
    private String targetUrl;
}
