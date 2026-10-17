package com.project.kfpcl_exports.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class OtpService {

    private static final int OTP_TTL_SECONDS = 300; // 5 minutes
    private static final int COOLDOWN_SECONDS = 15; // 15 seconds resend cooldown
    private static final int RATE_LIMIT_MAX_REQUESTS = 10; // 10 requests allowed per 10 minutes
    private static final int RATE_LIMIT_WINDOW_SECONDS = 600; // 10 minutes
    private static final int MAX_FAILED_ATTEMPTS = 5; // Brute force protection

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, List<Instant>> rateLimitStorage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Getter
    public static class OtpData {
        private final String otp;
        private final Instant createdAt;
        private final Instant expiresAt;
        private final AtomicInteger failedAttempts = new AtomicInteger(0);

        public OtpData(String otp, Instant createdAt, Instant expiresAt) {
            this.otp = otp;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public String getOtp() { return otp; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public int getFailedAttempts() { return failedAttempts.get(); }
        public int incrementFailedAttempts() { return failedAttempts.incrementAndGet(); }
    }

    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) return "****";
        return trimmed.substring(0, Math.min(2, trimmed.length())) + "******" + trimmed.substring(trimmed.length() - 2);
    }

    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String clean = phoneNumber.replaceAll("[^0-9]", "");
        if (clean.length() > 10) {
            clean = clean.substring(clean.length() - 10);
        }
        return clean.isEmpty() ? phoneNumber.trim() : clean;
    }

    public String generateAndSendOtp(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        Instant now = Instant.now();

        // 1. Rate Limiting Check
        List<Instant> timestamps = rateLimitStorage.computeIfAbsent(normalizedPhone, k -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> t.isBefore(now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS)));
            if (timestamps.size() >= RATE_LIMIT_MAX_REQUESTS) {
                throw new IllegalStateException("Too many OTP requests. Maximum " + RATE_LIMIT_MAX_REQUESTS + " requests allowed per 10 minutes.");
            }
        }

        // 2. Cooldown Window Check
        OtpData existingOtp = otpStorage.get(normalizedPhone);
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
        otpStorage.put(normalizedPhone, newOtpData);

        synchronized (timestamps) {
            timestamps.add(now);
        }

        log.info("[OTP SERVICE] Generated OTP for phone: {}", maskPhoneNumber(normalizedPhone));

        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String otpInput) {
        if (otpInput == null || otpInput.isBlank()) {
            return false;
        }

        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        OtpData otpData = otpStorage.get(normalizedPhone);
        if (otpData == null) {
            return false;
        }

        if (Instant.now().isAfter(otpData.getExpiresAt())) {
            otpStorage.remove(normalizedPhone);
            return false;
        }

        boolean matches = otpData.getOtp().equals(otpInput.trim());
        if (matches) {
            otpStorage.remove(normalizedPhone); // Single-use OTP
            return true;
        } else {
            int attempts = otpData.incrementFailedAttempts();
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                otpStorage.remove(normalizedPhone); // Invalidate OTP after 5 failed attempts
                log.warn("[OTP SERVICE] Max OTP verification attempts exceeded for phone: {}. Invalidation triggered.", maskPhoneNumber(normalizedPhone));
            }
            return false;
        }
    }

    public OtpData getActiveOtpData(String phoneNumber) {
        return otpStorage.get(normalizePhoneNumber(phoneNumber));
    }

    public void clearOtpStorage() {
        otpStorage.clear();
        rateLimitStorage.clear();
    }

    public long getOtpTtlSeconds() {
        return OTP_TTL_SECONDS;
    }
}
