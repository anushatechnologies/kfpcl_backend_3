package com.project.kfpcl_exports.loan.controller;

import com.project.kfpcl_exports.loan.dto.LoanApplicationRequest;
import com.project.kfpcl_exports.loan.dto.LoanApplicationResponse;
import com.project.kfpcl_exports.loan.service.LoanApplicationService;
import com.project.kfpcl_exports.loan.service.LoanAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationController.class);

    private final LoanApplicationService loanApplicationService;
    private final LoanAuthService loanAuthService;

    /**
     * Customer submits a loan application with documents.
     * Uploads documents directly to private Supabase Storage bucket through S3-compatible API.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitLoanApplication(
            @RequestParam("applicantName") String applicantName,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam("loanAmount") Double loanAmount,
            @RequestParam("loanPurpose") String loanPurpose,
            @RequestParam(value = "annualIncome", required = false) Double annualIncome,
            @RequestParam(value = "landSizeAcres", required = false) Double landSizeAcres,
            @RequestParam(value = "cropType", required = false) String cropType,
            @RequestParam(value = "farmLocation", required = false) String farmLocation,
            @RequestParam(value = "aadhaar", required = false) MultipartFile aadhaar,
            @RequestParam(value = "aadhaarFile", required = false) MultipartFile aadhaarFile,
            @RequestParam(value = "aadhaarDoc", required = false) MultipartFile aadhaarDoc,
            @RequestParam(value = "pan", required = false) MultipartFile pan,
            @RequestParam(value = "panFile", required = false) MultipartFile panFile,
            @RequestParam(value = "panDoc", required = false) MultipartFile panDoc,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "bankPassbook", required = false) MultipartFile bankPassbook,
            @RequestParam(value = "bank-passbook", required = false) MultipartFile bankPassbookHyphen,
            @RequestParam(value = "bankPassbookDoc", required = false) MultipartFile bankPassbookDoc,
            @RequestParam(value = "landRecords", required = false) MultipartFile landRecords,
            @RequestParam(value = "land-records", required = false) MultipartFile landRecordsHyphen,
            @RequestParam(value = "landRecordsDoc", required = false) MultipartFile landRecordsDoc,
            @RequestParam(value = "cropDetails", required = false) MultipartFile cropDetails,
            @RequestParam(value = "crop-details", required = false) MultipartFile cropDetailsHyphen,
            @RequestParam(value = "cropDetailsDoc", required = false) MultipartFile cropDetailsDoc,
            @RequestParam(value = "leaseDocuments", required = false) MultipartFile leaseDocuments,
            @RequestParam(value = "lease-documents", required = false) MultipartFile leaseDocumentsHyphen,
            @RequestParam(value = "leaseDocumentsDoc", required = false) MultipartFile leaseDocumentsDoc,
            HttpServletRequest request
    ) {
        try {
            LoanApplicationRequest req = LoanApplicationRequest.builder()
                    .applicantName(applicantName)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .loanAmount(loanAmount)
                    .loanPurpose(loanPurpose)
                    .annualIncome(annualIncome)
                    .landSizeAcres(landSizeAcres)
                    .cropType(cropType)
                    .farmLocation(farmLocation)
                    .build();

            Map<String, MultipartFile> documentFiles = new LinkedHashMap<>();

            // Aggregate named files with fallback alternatives
            addIfPresent(documentFiles, "aadhaar", aadhaar, aadhaarFile, aadhaarDoc);
            addIfPresent(documentFiles, "pan", pan, panFile, panDoc);
            addIfPresent(documentFiles, "photo", photo, photoFile);
            addIfPresent(documentFiles, "bank-passbook", bankPassbook, bankPassbookHyphen, bankPassbookDoc);
            addIfPresent(documentFiles, "land-records", landRecords, landRecordsHyphen, landRecordsDoc);
            addIfPresent(documentFiles, "crop-details", cropDetails, cropDetailsHyphen, cropDetailsDoc);
            addIfPresent(documentFiles, "lease-documents", leaseDocuments, leaseDocumentsHyphen, leaseDocumentsDoc);

            // Also check if any additional files were uploaded in MultipartHttpServletRequest
            if (request instanceof MultipartHttpServletRequest multipartRequest) {
                for (String partName : multipartRequest.getFileMap().keySet()) {
                    MultipartFile file = multipartRequest.getFile(partName);
                    if (file != null && !file.isEmpty() && !documentFiles.containsValue(file)) {
                        documentFiles.putIfAbsent(partName, file);
                    }
                }
            }

            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationResponse response = loanApplicationService.submitApplication(req, documentFiles, identity);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Loan application and documents submitted successfully.",
                    "data", response
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for loan application: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to process loan application: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Submission failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Customer retrieves their own loan applications.
     */
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(HttpServletRequest request) {
        LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
        if (identity.customerId() == null && identity.phoneNumber() == null && !identity.isAdmin()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", "Authentication required to view your loan applications."
            ));
        }

        List<LoanApplicationResponse> applications = loanApplicationService.getMyApplications(identity);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", applications
        ));
    }

    /**
     * Get loan application by ID (customer can only access their own).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationResponse app = loanApplicationService.getApplicationById(id, identity);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", app
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Download individual document through backend proxy.
     * Enforces authentication and authorization.
     */
    @GetMapping("/{applicationId}/documents/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable("applicationId") Long applicationId,
            @PathVariable("documentId") Long documentId,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationService.DocumentDownloadResult result =
                    loanApplicationService.getDocumentStream(applicationId, documentId, identity);

            MediaType mediaType;
            try {
                mediaType = (result.contentType() != null) ? MediaType.parseMediaType(result.contentType()) : MediaType.APPLICATION_OCTET_STREAM;
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(result.data().length);
            headers.setContentDisposition(ContentDisposition.attachment().filename(result.filename()).build());

            return new ResponseEntity<>(result.data(), headers, HttpStatus.OK);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error streaming document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to retrieve document: " + e.getMessage()));
        }
    }

    /**
     * Generate short-lived presigned download URL for direct S3 download.
     */
    @GetMapping("/{applicationId}/documents/{documentId}/presigned-url")
    public ResponseEntity<?> getPresignedUrl(
            @PathVariable("applicationId") Long applicationId,
            @PathVariable("documentId") Long documentId,
            @RequestParam(value = "expiryMinutes", defaultValue = "15") int expiryMinutes,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            String url = loanApplicationService.getPresignedUrl(applicationId, documentId, expiryMinutes, identity);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "presignedUrl", url,
                    "expiryMinutes", expiryMinutes
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private void addIfPresent(Map<String, MultipartFile> map, String key, MultipartFile... files) {
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                map.put(key, f);
                return;
            }
        }
    }
}
