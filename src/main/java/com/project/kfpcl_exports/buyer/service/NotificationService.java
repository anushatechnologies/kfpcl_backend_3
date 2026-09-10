package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.NotificationResponseDto;
import com.project.kfpcl_exports.buyer.enums.NotificationType;
import com.project.kfpcl_exports.buyer.exception.ResourceNotFoundException;
import com.project.kfpcl_exports.buyer.model.Notification;
import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
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
        List<Notification> list;
        try {
            list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        } catch (Exception ex) {
            log.warn("Standard notifications query failed ({}), falling back to native query", ex.getMessage());
            list = notificationRepository.findByUserIdNative(user != null ? user.getId() : "");
        }
        return list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Mark a single notification belonging to the buyer as read.
     */
    public void markNotificationRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (notification.getUser() != null && !notification.getUser().getId().equals(user.getId())) {
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
        try {
            notificationRepository.markAllAsReadForUser(user);
        } catch (Exception ignored) {}
    }

    /**
     * Get count of unread notifications for the buyer.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        try {
            return notificationRepository.countByUserAndIsReadFalse(user);
        } catch (Exception ex) {
            log.warn("Standard notification count failed ({}), falling back to native query", ex.getMessage());
            return notificationRepository.countByUserIdAndIsReadFalseNative(user != null ? user.getId() : "");
        }
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
