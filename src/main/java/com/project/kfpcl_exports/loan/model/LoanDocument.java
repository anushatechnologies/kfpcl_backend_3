package com.project.kfpcl_exports.loan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_documents")
public class LoanDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "document_category", nullable = false, length = 50)
    private String documentCategory;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp;

    @Column(name = "upload_status", nullable = false, length = 30)
    private String uploadStatus;

    public LoanDocument() {
    }

    public LoanDocument(Long id, LoanApplication loanApplication, String documentCategory,
                        String originalFilename, String objectKey, String contentType,
                        Long fileSize, LocalDateTime uploadTimestamp, String uploadStatus) {
        this.id = id;
        this.loanApplication = loanApplication;
        this.documentCategory = documentCategory;
        this.originalFilename = originalFilename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadTimestamp = uploadTimestamp;
        this.uploadStatus = uploadStatus;
    }

    @PrePersist
    protected void onCreate() {
        if (uploadTimestamp == null) {
            uploadTimestamp = LocalDateTime.now();
        }
        if (uploadStatus == null) {
            uploadStatus = "UPLOADED";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }

    public String getDocumentCategory() { return documentCategory; }
    public void setDocumentCategory(String documentCategory) { this.documentCategory = documentCategory; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }

    public static LoanDocumentBuilder builder() {
        return new LoanDocumentBuilder();
    }

    public static class LoanDocumentBuilder {
        private Long id;
        private LoanApplication loanApplication;
        private String documentCategory;
        private String originalFilename;
        private String objectKey;
        private String contentType;
        private Long fileSize;
        private LocalDateTime uploadTimestamp;
        private String uploadStatus;

        public LoanDocumentBuilder id(Long id) { this.id = id; return this; }
        public LoanDocumentBuilder loanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; return this; }
        public LoanDocumentBuilder documentCategory(String documentCategory) { this.documentCategory = documentCategory; return this; }
        public LoanDocumentBuilder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public LoanDocumentBuilder objectKey(String objectKey) { this.objectKey = objectKey; return this; }
        public LoanDocumentBuilder contentType(String contentType) { this.contentType = contentType; return this; }
        public LoanDocumentBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public LoanDocumentBuilder uploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; return this; }
        public LoanDocumentBuilder uploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; return this; }

        public LoanDocument build() {
            return new LoanDocument(id, loanApplication, documentCategory, originalFilename, objectKey, contentType, fileSize, uploadTimestamp, uploadStatus);
        }
    }
}
