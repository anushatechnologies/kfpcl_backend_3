package com.project.kfpcl_exports.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "fcm_token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "device_type", length = 20)
    private String deviceType = "ANDROID";

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public FcmToken() {
    }

    public FcmToken(Long id, User user, String fcmToken, String deviceType, LocalDateTime lastUpdated) {
        this.id = id;
        this.user = user;
        this.fcmToken = fcmToken;
        this.deviceType = deviceType != null ? deviceType : "ANDROID";
        this.lastUpdated = lastUpdated;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public static FcmTokenBuilder builder() {
        return new FcmTokenBuilder();
    }

    public static class FcmTokenBuilder {
        private Long id;
        private User user;
        private String fcmToken;
        private String deviceType = "ANDROID";
        private LocalDateTime lastUpdated;

        public FcmTokenBuilder id(Long id) { this.id = id; return this; }
        public FcmTokenBuilder user(User user) { this.user = user; return this; }
        public FcmTokenBuilder fcmToken(String fcmToken) { this.fcmToken = fcmToken; return this; }
        public FcmTokenBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public FcmTokenBuilder lastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; return this; }

        public FcmToken build() {
            return new FcmToken(id, user, fcmToken, deviceType, lastUpdated);
        }
    }
}
