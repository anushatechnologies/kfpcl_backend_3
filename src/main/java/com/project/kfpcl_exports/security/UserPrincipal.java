package com.project.kfpcl_exports.security;

import lombok.Getter;

@Getter
public class UserPrincipal {
    private final Long userId;
    private final String buyerId;
    private final String phoneNumber;
    private final String accessToken;

    public UserPrincipal(Long userId, String phoneNumber, String accessToken) {
        this.userId = userId;
        this.buyerId = userId != null ? String.valueOf(userId) : null;
        this.phoneNumber = phoneNumber;
        this.accessToken = accessToken;
    }

    public UserPrincipal(String buyerId, String phoneNumber, String accessToken) {
        this.buyerId = buyerId;
        Long parsedId = null;
        try {
            parsedId = Long.parseLong(buyerId);
        } catch (Exception ignored) {}
        this.userId = parsedId;
        this.phoneNumber = phoneNumber;
        this.accessToken = accessToken;
    }

    public UserPrincipal(Long userId, String buyerId, String phoneNumber, String accessToken) {
        this.userId = userId;
        this.buyerId = buyerId;
        this.phoneNumber = phoneNumber;
        this.accessToken = accessToken;
    }
}
