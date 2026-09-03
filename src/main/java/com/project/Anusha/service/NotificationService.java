package com.project.Anusha.service;

import com.project.Anusha.dto.NotificationResponseDto;
import com.project.Anusha.enums.NotificationType;
import com.project.Anusha.exception.ResourceNotFoundException;
import com.project.Anusha.model.Notification;
import com.project.Anusha.model.User;
import com.project.Anusha.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Creates and persists a notification for a user.
     */
    public Notification createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            String referenceId
    ) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
                .user(user)
                .type(type != null ? type : NotificationType.GENERAL)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .createdAt(now)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Return all notifications belonging strictly to the authenticated buyer.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getBuyerNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Mark a single notification belonging to the buyer as read.
     */
    public void markNotificationRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications belonging strictly to the authenticated buyer as read.
     */
    public void markAllNotificationsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    /**
     * Get count of unread notifications for the buyer.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    private NotificationResponseDto mapToDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
