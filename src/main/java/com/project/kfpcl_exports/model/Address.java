package com.project.kfpcl_exports.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_type", length = 50)
    private String addressType = "Warehouse";

    @Column(name = "house_no", length = 100)
    private String houseNo;

    @Column(name = "street_details", nullable = false, columnDefinition = "TEXT")
    private String streetDetails;

    @Column(name = "landmark", length = 120)
    private String landmark;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "state", nullable = false, length = 80)
    private String state;

    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Address() {
    }

    public Address(Long id, User user, String addressType, String houseNo, String streetDetails,
                   String landmark, String city, String state, String pincode, Boolean isDefault,
                   LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.addressType = addressType != null ? addressType : "Warehouse";
        this.houseNo = houseNo;
        this.streetDetails = streetDetails;
        this.landmark = landmark;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.isDefault = isDefault != null ? isDefault : false;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }

    public String getHouseNo() { return houseNo; }
    public void setHouseNo(String houseNo) { this.houseNo = houseNo; }

    public String getStreetDetails() { return streetDetails; }
    public void setStreetDetails(String streetDetails) { this.streetDetails = streetDetails; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static AddressBuilder builder() {
        return new AddressBuilder();
    }

    public static class AddressBuilder {
        private Long id;
        private User user;
        private String addressType = "Warehouse";
        private String houseNo;
        private String streetDetails;
        private String landmark;
        private String city;
        private String state;
        private String pincode;
        private Boolean isDefault = false;
        private LocalDateTime createdAt;

        public AddressBuilder id(Long id) { this.id = id; return this; }
        public AddressBuilder user(User user) { this.user = user; return this; }
        public AddressBuilder addressType(String addressType) { this.addressType = addressType; return this; }
        public AddressBuilder houseNo(String houseNo) { this.houseNo = houseNo; return this; }
        public AddressBuilder streetDetails(String streetDetails) { this.streetDetails = streetDetails; return this; }
        public AddressBuilder landmark(String landmark) { this.landmark = landmark; return this; }
        public AddressBuilder city(String city) { this.city = city; return this; }
        public AddressBuilder state(String state) { this.state = state; return this; }
        public AddressBuilder pincode(String pincode) { this.pincode = pincode; return this; }
        public AddressBuilder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public AddressBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Address build() {
            return new Address(id, user, addressType, houseNo, streetDetails, landmark, city, state, pincode, isDefault, createdAt);
        }
    }
}
