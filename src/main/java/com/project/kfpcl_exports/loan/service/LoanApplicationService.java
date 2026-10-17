package com.project.kfpcl_exports.loan.service;

import com.project.kfpcl_exports.loan.dto.*;
import com.project.kfpcl_exports.loan.model.LoanApplication;
import com.project.kfpcl_exports.loan.model.LoanDocument;
import com.project.kfpcl_exports.loan.repository.LoanApplicationRepository;
import com.project.kfpcl_exports.loan.repository.LoanDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationService.class);

    private static final Set<String> MANDATORY_CATEGORIES = Set.of(
            "aadhaar",
            "pan",
            "photo",
            "bank-passbook",
            "land-records"
    );

    private static final Set<String> ALL_SUPPORTED_CATEGORIES = Set.of(
            "aadhaar",
            "pan",
            "photo",
            "bank-passbook",
            "land-records",
            "crop-details",
            "lease-documents"
    );

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;
    private final LoanDocumentStorageService storageService;
    private final LoanPdfService loanPdfService;
    private final LoanAuthService loanAuthService;

    /**
     * Submit loan application with files.
     * Safely handles partial failure: if any document fails to upload, already uploaded files are removed
     * from Supabase storage and the transaction is aborted.
     */
    @Transactional
    public LoanApplicationResponse submitApplication(
            LoanApplicationRequest request,
            Map<String, MultipartFile> documentFiles,
            LoanAuthService.CurrentIdentity identity
    ) {
        log.info("Processing loan application submission for applicant: '{}', phone: '{}'",
                request.getApplicantName(), request.getPhoneNumber());

        // 1. Validate mandatory documents
        validateDocumentsPresent(documentFiles);

        // 2. Generate unique Application Number
        String appNumber = generateApplicationNumber();

        // 3. Resolve customer ID
        String customerId = resolveCustomerId(request, identity);

        // 4. Create LoanApplication Entity
        LoanApplication application = LoanApplication.builder()
                .applicationNumber(appNumber)
                .customerId(customerId)
                .applicantName(request.getApplicantName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .loanAmount(request.getLoanAmount())
                .loanPurpose(request.getLoanPurpose())
                .annualIncome(request.getAnnualIncome())
                .landSizeAcres(request.getLandSizeAcres())
                .cropType(request.getCropType())
                .farmLocation(request.getFarmLocation())
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. Upload each document to Supabase S3 bucket with partial failure rollback
        List<String> uploadedObjectKeys = new ArrayList<>();
        List<LoanDocument> loanDocuments = new ArrayList<>();

        try {
            for (Map.Entry<String, MultipartFile> entry : documentFiles.entrySet()) {
                String rawCategory = entry.getKey();
                MultipartFile file = entry.getValue();

                if (file == null || file.isEmpty()) {
                    continue;
                }

                String normalizedCategory = normalizeCategory(rawCategory);

                log.info("Uploading document category '{}' for application '{}'", normalizedCategory, appNumber);
                LoanDocumentStorageService.StoredDocumentResult stored = storageService.uploadDocument(appNumber, normalizedCategory, file);
                uploadedObjectKeys.add(stored.objectKey());

                LoanDocument docEntity = LoanDocument.builder()
                        .documentCategory(stored.category())
                        .originalFilename(stored.originalFilename())
                        .objectKey(stored.objectKey())
                        .contentType(stored.contentType())
                        .fileSize(stored.fileSize())
                        .uploadTimestamp(stored.uploadTimestamp())
                        .uploadStatus(stored.uploadStatus())
                        .build();

                application.addDocument(docEntity);
                loanDocuments.add(docEntity);
            }

            // 6. Save in database
            LoanApplication savedApplication = loanApplicationRepository.save(application);
            log.info("Loan application '{}' successfully created with {} documents in Supabase Storage",
                    savedApplication.getApplicationNumber(), savedApplication.getDocuments().size());

            return mapToResponse(savedApplication);

        } catch (Exception ex) {
            log.error("Error during document upload/saving for application '{}'. Rolling back {} uploaded objects from Supabase Storage.",
                    appNumber, uploadedObjectKeys.size(), ex);

            // Rollback: Clean up any already uploaded objects from Supabase bucket
            storageService.deleteObjects(uploadedObjectKeys);

            throw new RuntimeException("Loan application submission failed during storage upload: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get loan application by ID.
     */
    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplicationById(Long id, LoanAuthService.CurrentIdentity identity) {
        LoanApplication app = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + id));

        if (!loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to view this loan application.");
        }

        return mapToResponse(app);
    }

    /**
     * List applications for the current authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getMyApplications(LoanAuthService.CurrentIdentity identity) {
        Set<LoanApplication> result = new LinkedHashSet<>();

        if (StringUtils.hasText(identity.customerId())) {
            result.addAll(loanApplicationRepository.findByCustomerIdOrderByCreatedAtDesc(identity.customerId()));
        }
        if (StringUtils.hasText(identity.phoneNumber())) {
            result.addAll(loanApplicationRepository.findByPhoneNumberOrderByCreatedAtDesc(identity.phoneNumber()));
        }

        return result.stream().map(this::mapToResponse).toList();
    }

    /**
     * Admin: List all applications with pagination and filters.
     */
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getAllApplicationsAdmin(
            String status,
            String search,
            Pageable pageable,
            LoanAuthService.CurrentIdentity identity
    ) {
        if (!identity.isAdmin()) {
            throw new AccessDeniedException("Access denied: Admin role required to view all applications.");
        }

        String filterStatus = StringUtils.hasText(status) ? status.trim() : null;
        String filterSearch = StringUtils.hasText(search) ? search.trim() : null;

        return loanApplicationRepository.findAllFiltered(filterStatus, filterSearch, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Admin: Update loan application status.
     */
    @Transactional
    public LoanApplicationResponse updateStatusAdmin(
            Long id,
            UpdateLoanStatusRequest request,
            LoanAuthService.CurrentIdentity identity
    ) {
        if (!identity.isAdmin()) {
            throw new AccessDeniedException("Access denied: Admin role required to update application status.");
        }

        LoanApplication app = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + id));

        app.setStatus(request.getStatus().toUpperCase());
        if (request.getRejectionReason() != null) {
            app.setRejectionReason(request.getRejectionReason());
        }
        if (request.getAdminNotes() != null) {
            app.setAdminNotes(request.getAdminNotes());
        }

        LoanApplication updated = loanApplicationRepository.save(app);
        return mapToResponse(updated);
    }

    /**
     * Stream document bytes directly from private Supabase bucket.
     */
    @Transactional(readOnly = true)
    public DocumentDownloadResult getDocumentStream(Long applicationId, Long documentId, LoanAuthService.CurrentIdentity identity) throws IOException {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + applicationId));

        if (!loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to download documents from this application.");
        }

        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!doc.getLoanApplication().getId().equals(applicationId)) {
            throw new IllegalArgumentException("Document does not belong to application: " + applicationId);
        }

        byte[] data = storageService.getObjectBytes(doc.getObjectKey());
        return new DocumentDownloadResult(doc.getOriginalFilename(), doc.getContentType(), data);
    }

    /**
     * Generate short-lived presigned URL for secure direct download from S3.
     */
    @Transactional(readOnly = true)
    public String getPresignedUrl(Long applicationId, Long documentId, int expiryMinutes, LoanAuthService.CurrentIdentity identity) {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + applicationId));

        if (!loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to access documents from this application.");
        }

        LoanDocument doc = loanDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!doc.getLoanApplication().getId().equals(applicationId)) {
            throw new IllegalArgumentException("Document does not belong to application: " + applicationId);
        }

        return storageService.generatePresignedDownloadUrl(doc.getObjectKey(), expiryMinutes);
    }

    /**
     * Admin: Stream all application documents as a ZIP archive.
     */
    @Transactional(readOnly = true)
    public void downloadAllAsZip(Long applicationId, OutputStream outputStream, LoanAuthService.CurrentIdentity identity) throws IOException {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + applicationId));

        if (!identity.isAdmin() && !loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to download this application's documents.");
        }

        List<LoanDocument> docs = loanDocumentRepository.findByLoanApplicationId(applicationId);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            // Also include application summary PDF inside the ZIP
            try {
                byte[] summaryPdf = loanPdfService.generateSummaryPdf(app);
                ZipEntry summaryEntry = new ZipEntry("00_Application_Summary_" + app.getApplicationNumber() + ".pdf");
                zos.putNextEntry(summaryEntry);
                zos.write(summaryPdf);
                zos.closeEntry();
            } catch (Exception e) {
                log.warn("Failed to generate summary PDF for ZIP package: {}", e.getMessage());
            }

            int index = 1;
            for (LoanDocument doc : docs) {
                try {
                    byte[] fileBytes = storageService.getObjectBytes(doc.getObjectKey());
                    String entryName = String.format("%02d_%s_%s", index++, doc.getDocumentCategory(), doc.getOriginalFilename());
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    zos.write(fileBytes);
                    zos.closeEntry();
                } catch (Exception ex) {
                    log.error("Failed to add document key '{}' to zip: {}", doc.getObjectKey(), ex.getMessage());
                }
            }
            zos.finish();
        }
    }

    /**
     * Admin: Generate combined PDF merging all application documents.
     */
    @Transactional(readOnly = true)
    public byte[] generateCombinedPdf(Long applicationId, LoanAuthService.CurrentIdentity identity) throws IOException {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + applicationId));

        if (!identity.isAdmin() && !loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to generate combined PDF for this application.");
        }

        List<LoanDocument> docs = loanDocumentRepository.findByLoanApplicationId(applicationId);
        List<LoanPdfService.LoanDocumentData> docDataList = new ArrayList<>();

        for (LoanDocument doc : docs) {
            try {
                byte[] data = storageService.getObjectBytes(doc.getObjectKey());
                docDataList.add(new LoanPdfService.LoanDocumentData(
                        doc.getDocumentCategory(),
                        doc.getOriginalFilename(),
                        doc.getContentType(),
                        data
                ));
            } catch (Exception ex) {
                log.warn("Failed to fetch document for key '{}' during combined PDF generation: {}", doc.getObjectKey(), ex.getMessage());
            }
        }

        return loanPdfService.generateCombinedPdf(app, docDataList);
    }

    /**
     * Admin: Generate application summary PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateSummaryPdf(Long applicationId, LoanAuthService.CurrentIdentity identity) throws IOException {
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found with ID: " + applicationId));

        if (!identity.isAdmin() && !loanAuthService.canAccessApplication(identity, app)) {
            throw new AccessDeniedException("Access denied: You are not authorized to generate summary PDF for this application.");
        }

        return loanPdfService.generateSummaryPdf(app);
    }

    public record DocumentDownloadResult(
            String filename,
            String contentType,
            byte[] data
    ) {}

    private void validateDocumentsPresent(Map<String, MultipartFile> documentFiles) {
        if (documentFiles == null || documentFiles.isEmpty()) {
            throw new IllegalArgumentException("No documents provided. Loan application requires documents: " + String.join(", ", MANDATORY_CATEGORIES));
        }

        Set<String> normalizedProvided = new HashSet<>();
        for (String key : documentFiles.keySet()) {
            MultipartFile file = documentFiles.get(key);
            if (file != null && !file.isEmpty()) {
                normalizedProvided.add(normalizeCategory(key));
            }
        }

        List<String> missing = new ArrayList<>();
        for (String mandatory : MANDATORY_CATEGORIES) {
            if (!normalizedProvided.contains(mandatory)) {
                missing.add(mandatory);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required loan documents: " + String.join(", ", missing));
        }
    }

    private String normalizeCategory(String raw) {
        if (!StringUtils.hasText(raw)) return "general";
        String clean = raw.toLowerCase(Locale.ROOT).trim()
                .replace("_", "-")
                .replaceAll("file|doc|document", "")
                .replaceAll("-+$", "");

        return switch (clean) {
            case "aadhaar", "aadhar", "aadhaardoc", "aadhaarfile" -> "aadhaar";
            case "pan", "pancard", "pandoc", "panfile" -> "pan";
            case "photo", "photofile", "applicantphoto" -> "photo";
            case "bank-passbook", "bankpassbook", "passbook", "bankstatement" -> "bank-passbook";
            case "land-records", "landrecords", "landrecord", "patta", "chitta" -> "land-records";
            case "crop-details", "cropdetails", "crop" -> "crop-details";
            case "lease-documents", "leasedocuments", "leaseagreement", "lease" -> "lease-documents";
            default -> clean;
        };
    }

    private String generateApplicationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return String.format("KFPCL-LN-%s-%s", datePart, randomHex);
    }

    private String resolveCustomerId(LoanApplicationRequest request, LoanAuthService.CurrentIdentity identity) {
        if (StringUtils.hasText(identity.customerId())) {
            return identity.customerId();
        }
        String cleanPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");
        return cleanPhone.length() > 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone;
    }

    public LoanApplicationResponse mapToResponse(LoanApplication app) {
        List<LoanDocumentDto> docDtos = new ArrayList<>();
        if (app.getDocuments() != null) {
            for (LoanDocument doc : app.getDocuments()) {
                String downloadUrl = String.format("/api/loan-applications/%d/documents/%d/download", app.getId(), doc.getId());
                docDtos.add(LoanDocumentDto.builder()
                        .id(doc.getId())
                        .applicationId(app.getId())
                        .documentCategory(doc.getDocumentCategory())
                        .originalFilename(doc.getOriginalFilename())
                        .objectKey(doc.getObjectKey())
                        .contentType(doc.getContentType())
                        .fileSize(doc.getFileSize())
                        .uploadTimestamp(doc.getUploadTimestamp())
                        .uploadStatus(doc.getUploadStatus())
                        .downloadUrl(downloadUrl)
                        .build());
            }
        }

        return LoanApplicationResponse.builder()
                .id(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .userId(app.getUserId())
                .customerId(app.getCustomerId())
                .applicantName(app.getApplicantName())
                .phoneNumber(app.getPhoneNumber())
                .email(app.getEmail())
                .loanAmount(app.getLoanAmount())
                .loanPurpose(app.getLoanPurpose())
                .annualIncome(app.getAnnualIncome())
                .landSizeAcres(app.getLandSizeAcres())
                .cropType(app.getCropType())
                .farmLocation(app.getFarmLocation())
                .status(app.getStatus())
                .rejectionReason(app.getRejectionReason())
                .adminNotes(app.getAdminNotes())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .documents(docDtos)
                .build();
    }
}
