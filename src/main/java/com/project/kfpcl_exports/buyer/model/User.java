package com.project.kfpcl_exports.buyer.model;

import com.project.kfpcl_exports.buyer.enums.BusinessType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity(name = "BuyerUser")
@Table(name = "buyer_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Helper getter so existing Java code calling user.getName() works without needing a 'name' column in DB
    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = true)
    private String password;

    @Builder.Default
    @Column(name = "company_name", length = 150)
    private String companyName = "KFPCL Buyer";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 20)
    private BusinessType businessType = BusinessType.WHOLESALER;

    @Builder.Default
    @Column(length = 100)
    private String state = "India";

    @Builder.Default
    @Column(length = 100)
    private String city = "India";

    // --- GSTIN Fields (Optional) ---
    @Column(length = 15)
    private String gstin;

    @Column(name = "gstin_photo_url", length = 500)
    private String gstinPhotoUrl;

    @Column(name = "gstin_photo_original_name")
    private String gstinPhotoOriginalName;

    @Column(name = "gstin_photo_mime_type", length = 50)
    private String gstinPhotoMimeType;

    @Column(name = "gstin_photo_file_size")
    private Long gstinPhotoFileSize;

    // --- PAN Card Fields (Required) ---
    @Column(name = "pan_number", unique = true, length = 10)
    private String panNumber;

    @Column(name = "pan_card_url", length = 500)
    private String panCardUrl;

    @Column(name = "pan_card_original_name")
    private String panCardOriginalName;

    @Column(name = "pan_card_mime_type", length = 50)
    private String panCardMimeType;

    @Column(name = "pan_card_file_size")
    private Long panCardFileSize;

    @Builder.Default
    @Column(name = "status", length = 30)
    private String status = "PENDING_VERIFICATION";

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String role = "ROLE_USER";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void ensureDefaults() {
        if (city == null || city.isBlank()) {
            city = "India";
        }
        if (state == null || state.isBlank()) {
            state = "India";
        }
        if (companyName == null || companyName.isBlank()) {
            companyName = (fullName != null && !fullName.isBlank()) ? fullName : "KFPCL Buyer";
        }
        if (status == null || status.isBlank()) {
            status = "PENDING_VERIFICATION";
        }
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        }
        if (businessType == null) {
            businessType = BusinessType.WHOLESALER;
        }
    }
}
