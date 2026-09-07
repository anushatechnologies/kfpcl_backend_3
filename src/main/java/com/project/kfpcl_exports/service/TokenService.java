package com.project.kfpcl_exports.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final long VERIFICATION_TOKEN_TTL_SECONDS = 15 * 60; // 15 mins
    private static final long ACCESS_TOKEN_TTL_SECONDS = 15 * 60; // 15 mins
    private static final long REFRESH_TOKEN_TTL_SECONDS = 30L * 24 * 60 * 60; // 30 days

    private final Map<String, VerificationTokenData> verificationTokens = new ConcurrentHashMap<>();
    private final Map<String, AccessTokenData> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, RefreshTokenData> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Boolean> tokenBlacklist = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class VerificationTokenData {
        private final String phoneNumber;
        private final Instant expiresAt;
    }

    @Getter
    @AllArgsConstructor
    public static class AccessTokenData {
        private final Long userId;
        private final String phoneNumber;
        private final Instant expiresAt;
    }

    @Getter
    @AllArgsConstructor
    public static class RefreshTokenData {
        private final Long userId;
        private final String phoneNumber;
        private final Instant expiresAt;
    }

    public String createVerificationToken(String phoneNumber) {
        String token = "temp_verif_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(VERIFICATION_TOKEN_TTL_SECONDS);
        verificationTokens.put(token, new VerificationTokenData(phoneNumber, expiresAt));
        return token;
    }

    public boolean validateVerificationToken(String token, String phoneNumber) {
        VerificationTokenData data = verificationTokens.get(token);
        if (data == null) {
            return false;
        }
        if (Instant.now().isAfter(data.getExpiresAt())) {
            verificationTokens.remove(token);
            return false;
        }
        boolean matches = data.getPhoneNumber().equals(phoneNumber);
        if (matches) {
            verificationTokens.remove(token); // One-time use
        }
        return matches;
    }

    public String createAccessToken(Long userId, String phoneNumber) {
        String token = "acc_tok_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS);
        accessTokens.put(token, new AccessTokenData(userId, phoneNumber, expiresAt));
        return token;
    }

    public String createRefreshToken(Long userId, String phoneNumber) {
        String token = "ref_tok_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS);
        refreshTokens.put(token, new RefreshTokenData(userId, phoneNumber, expiresAt));
        return token;
    }

    public AccessTokenData validateAccessToken(String token) {
        if (token == null || token.isBlank() || tokenBlacklist.containsKey(token)) {
            return null;
        }
        AccessTokenData data = accessTokens.get(token);
        if (data == null) {
            return null;
        }
        if (Instant.now().isAfter(data.getExpiresAt())) {
            accessTokens.remove(token);
            return null;
        }
        return data;
    }

    public RefreshTokenData validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        RefreshTokenData data = refreshTokens.get(refreshToken);
        if (data == null) {
            return null;
        }
        if (Instant.now().isAfter(data.getExpiresAt())) {
            refreshTokens.remove(refreshToken);
            return null;
        }
        return data;
    }

    public String rotateAccessToken(String refreshToken) {
        RefreshTokenData data = validateRefreshToken(refreshToken);
        if (data == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        return createAccessToken(data.getUserId(), data.getPhoneNumber());
    }

    public void invalidateSession(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            tokenBlacklist.put(accessToken, Boolean.TRUE);
            accessTokens.remove(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokens.remove(refreshToken);
        }
    }
}
