package com.project.Anusha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.Anusha.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}
