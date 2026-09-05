package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.ApiResponse;
import com.project.kfpcl_exports.buyer.dto.NotificationResponseDto;
import com.project.kfpcl_exports.buyer.dto.UnreadCountResponse;
import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.service.NotificationService;
import com.project.kfpcl_exports.buyer.util.BuyerAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("buyerNotificationController")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final BuyerAuthHelper buyerAuthHelper;

    public NotificationController(NotificationService notificationService, BuyerAuthHelper buyerAuthHelper) {
        this.notificationService = notificationService;
        this.buyerAuthHelper = buyerAuthHelper;
    }

    /**
     * 11. GET /api/notifications
     * Return notifications belonging only to logged-in buyer.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        List<NotificationResponseDto> response = notificationService.getBuyerNotifications(buyer);
        return ResponseEntity.ok(ApiResponse.ok("Notifications fetched successfully", response));
    }

    /**
     * 12. PATCH /api/notifications/{notificationId}/read
     * Mark only the logged-in buyer's notification as read.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        notificationService.markNotificationRead(buyer, notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    /**
     * 13. PATCH /api/notifications/read-all
     * Mark all notifications belonging to logged-in buyer as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        notificationService.markAllNotificationsRead(buyer);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }

    /**
     * 14. GET /api/notifications/unread-count
     * Return count of unread notifications for logged-in buyer.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        long count = notificationService.getUnreadCount(buyer);
        return ResponseEntity.ok(ApiResponse.ok("Unread count fetched successfully", new UnreadCountResponse(count)));
    }
}
