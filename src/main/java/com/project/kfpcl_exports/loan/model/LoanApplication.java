package com.project.kfpcl_exports.loan.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false, length = 64)
    private String applicationNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "applicant_name", nullable = false, length = 150)
    private String applicantName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "loan_amount", nullable = false)
    private Double loanAmount;

    @Column(name = "loan_purpose", length = 100)
    private String loanPurpose;

    @Column(name = "annual_income")
    private Double annualIncome;

    @Column(name = "land_size_acres")
    private Double landSizeAcres;

    @Column(name = "crop_type", length = 100)
    private String cropType;

    @Column(name = "farm_location", length = 255)
    private String farmLocation;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanDocument> documents = new ArrayList<>();

    public LoanApplication() {
        this.documents = new ArrayList<>();
    }

    public LoanApplication(Long id, String applicationNumber, Long userId, String customerId,
                           String applicantName, String phoneNumber, String email, Double loanAmount,
                           String loanPurpose, Double annualIncome, Double landSizeAcres,
                           String cropType, String farmLocation, String status, String rejectionReason,
                           String adminNotes, LocalDateTime createdAt, LocalDateTime updatedAt,
                           List<LoanDocument> documents) {
        this.id = id;
        this.applicationNumber = applicationNumber;
        this.userId = userId;
        this.customerId = customerId;
        this.applicantName = applicantName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.loanAmount = loanAmount;
        this.loanPurpose = loanPurpose;
        this.annualIncome = annualIncome;
        this.landSizeAcres = landSizeAcres;
        this.cropType = cropType;
        this.farmLocation = farmLocation;
        this.status = status != null ? status : "SUBMITTED";
        this.rejectionReason = rejectionReason;
        this.adminNotes = adminNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "SUBMITTED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addDocument(LoanDocument doc) {
        if (documents == null) {
            documents = new ArrayList<>();
        }
        documents.add(doc);
        doc.setLoanApplication(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }

    public String getLoanPurpose() { return loanPurpose; }
    public void setLoanPurpose(String loanPurpose) { this.loanPurpose = loanPurpose; }

    public Double getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(Double annualIncome) { this.annualIncome = annualIncome; }

    public Double getLandSizeAcres() { return landSizeAcres; }
    public void setLandSizeAcres(Double landSizeAcres) { this.landSizeAcres = landSizeAcres; }

    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }

    public String getFarmLocation() { return farmLocation; }
    public void setFarmLocation(String farmLocation) { this.farmLocation = farmLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<LoanDocument> getDocuments() { return documents; }
    public void setDocuments(List<LoanDocument> documents) { this.documents = documents; }

    public static LoanApplicationBuilder builder() {
        return new LoanApplicationBuilder();
    }

    public static class LoanApplicationBuilder {
        private Long id;
        private String applicationNumber;
        private Long userId;
        private String customerId;
        private String applicantName;
        private String phoneNumber;
        private String email;
        private Double loanAmount;
        private String loanPurpose;
        private Double annualIncome;
        private Double landSizeAcres;
        private String cropType;
        private String farmLocation;
        private String status = "SUBMITTED";
        private String rejectionReason;
        private String adminNotes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<LoanDocument> documents = new ArrayList<>();

        public LoanApplicationBuilder id(Long id) { this.id = id; return this; }
        public LoanApplicationBuilder applicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; return this; }
        public LoanApplicationBuilder userId(Long userId) { this.userId = userId; return this; }
        public LoanApplicationBuilder customerId(String customerId) { this.customerId = customerId; return this; }
        public LoanApplicationBuilder applicantName(String applicantName) { this.applicantName = applicantName; return this; }
        public LoanApplicationBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public LoanApplicationBuilder email(String email) { this.email = email; return this; }
        public LoanApplicationBuilder loanAmount(Double loanAmount) { this.loanAmount = loanAmount; return this; }
        public LoanApplicationBuilder loanPurpose(String loanPurpose) { this.loanPurpose = loanPurpose; return this; }
        public LoanApplicationBuilder annualIncome(Double annualIncome) { this.annualIncome = annualIncome; return this; }
        public LoanApplicationBuilder landSizeAcres(Double landSizeAcres) { this.landSizeAcres = landSizeAcres; return this; }
        public LoanApplicationBuilder cropType(String cropType) { this.cropType = cropType; return this; }
        public LoanApplicationBuilder farmLocation(String farmLocation) { this.farmLocation = farmLocation; return this; }
        public LoanApplicationBuilder status(String status) { this.status = status; return this; }
        public LoanApplicationBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public LoanApplicationBuilder adminNotes(String adminNotes) { this.adminNotes = adminNotes; return this; }
        public LoanApplicationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public LoanApplicationBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public LoanApplicationBuilder documents(List<LoanDocument> documents) { this.documents = documents; return this; }

        public LoanApplication build() {
            return new LoanApplication(id, applicationNumber, userId, customerId, applicantName, phoneNumber,
                    email, loanAmount, loanPurpose, annualIncome, landSizeAcres, cropType, farmLocation,
                    status, rejectionReason, adminNotes, createdAt, updatedAt, documents);
        }
    }
}
