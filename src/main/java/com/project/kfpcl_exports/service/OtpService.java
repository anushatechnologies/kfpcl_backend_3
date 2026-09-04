package com.project.kfpcl_exports.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_TTL_SECONDS = 300; // 5 minutes
    private static final int COOLDOWN_SECONDS = 60; // 60 seconds resend cooldown
    private static final int RATE_LIMIT_MAX_REQUESTS = 3;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 600; // 10 minutes

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, List<Instant>> rateLimitStorage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Getter
    @AllArgsConstructor
    public static class OtpData {
        private final String otp;
        private final Instant createdAt;
        private final Instant expiresAt;

        public String getOtp() { return otp; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    public String generateAndSendOtp(String phoneNumber) {
        Instant now = Instant.now();

        // 1. Rate Limiting Check (Max 3 requests per 10 minutes)
        List<Instant> timestamps = rateLimitStorage.computeIfAbsent(phoneNumber, k -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> t.isBefore(now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS)));
            if (timestamps.size() >= RATE_LIMIT_MAX_REQUESTS) {
                throw new IllegalStateException("Too many OTP requests. Maximum 3 requests allowed per 10 minutes.");
            }
        }

        // 2. Cooldown Window Check (60 seconds)
        OtpData existingOtp = otpStorage.get(phoneNumber);
        if (existingOtp != null) {
            long secondsSinceLastRequest = now.getEpochSecond() - existingOtp.getCreatedAt().getEpochSecond();
            if (secondsSinceLastRequest < COOLDOWN_SECONDS) {
                long remainingCooldown = COOLDOWN_SECONDS - secondsSinceLastRequest;
                throw new IllegalStateException("Please wait " + remainingCooldown + " seconds before requesting a new OTP.");
            }
        }

        // 3. Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // 4. Store OTP with 300s TTL
        OtpData newOtpData = new OtpData(otp, now, now.plusSeconds(OTP_TTL_SECONDS));
        otpStorage.put(phoneNumber, newOtpData);

        synchronized (timestamps) {
            timestamps.add(now);
        }

        // In a production setup, SMS Gateway Webhook / SDK would be invoked here.
        System.out.println("[SMS OTP ENGINE] Sent OTP " + otp + " to phone: " + phoneNumber);

        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otpInput) {
        OtpData otpData = otpStorage.get(phoneNumber);
        if (otpData == null) {
            return false;
        }

        if (Instant.now().isAfter(otpData.getExpiresAt())) {
            otpStorage.remove(phoneNumber);
            return false;
        }

        boolean matches = otpData.getOtp().equals(otpInput);
        if (matches) {
            otpStorage.remove(phoneNumber); // Single-use OTP
        }
        return matches;
    }

    public OtpData getActiveOtpData(String phoneNumber) {
        return otpStorage.get(phoneNumber);
    }

    public void clearOtpStorage() {
        otpStorage.clear();
        rateLimitStorage.clear();
    }

    public long getOtpTtlSeconds() {
        return OTP_TTL_SECONDS;
    }
}
