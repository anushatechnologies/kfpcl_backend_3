package com.project.kfpcl_exports.loan.controller;

import com.project.kfpcl_exports.loan.dto.LoanApplicationResponse;
import com.project.kfpcl_exports.loan.dto.UpdateLoanStatusRequest;
import com.project.kfpcl_exports.loan.service.LoanApplicationService;
import com.project.kfpcl_exports.loan.service.LoanAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class AdminLoanApplicationController {

    private static final Logger log = LoggerFactory.getLogger(AdminLoanApplicationController.class);

    private final LoanApplicationService loanApplicationService;
    private final LoanAuthService loanAuthService;

    /**
     * Admin: List all loan applications with optional status and search filtering.
     */
    @GetMapping
    public ResponseEntity<?> getAllLoanApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<LoanApplicationResponse> result = loanApplicationService.getAllApplicationsAdmin(status, search, pageRequest, identity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.getContent(),
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages(),
                    "currentPage", result.getNumber()
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Admin: View specific loan application.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanApplication(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationResponse response = loanApplicationService.getApplicationById(id, identity);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Admin: Update loan application status (UNDER_REVIEW, APPROVED, REJECTED, DISBURSED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLoanApplicationStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateLoanStatusRequest updateRequest,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationResponse response = loanApplicationService.updateStatusAdmin(id, updateRequest, identity);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Loan application status updated successfully.",
                    "data", response
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Admin: Download an individual document retrieved from private Supabase Storage.
     */
    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable("id") Long id,
            @PathVariable("documentId") Long documentId,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            LoanApplicationService.DocumentDownloadResult result =
                    loanApplicationService.getDocumentStream(id, documentId, identity);

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
            log.error("Error streaming document for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to retrieve document: " + e.getMessage()));
        }
    }

    /**
     * Admin: Generate short-lived presigned download URL for direct S3 download.
     */
    @GetMapping("/{id}/documents/{documentId}/presigned-url")
    public ResponseEntity<?> getPresignedUrl(
            @PathVariable("id") Long id,
            @PathVariable("documentId") Long documentId,
            @RequestParam(value = "expiryMinutes", defaultValue = "30") int expiryMinutes,
            HttpServletRequest request
    ) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            String url = loanApplicationService.getPresignedUrl(id, documentId, expiryMinutes, identity);
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

    /**
     * Admin: Download all documents of an application as a ZIP archive.
     * Retrieved directly from private Supabase S3 bucket.
     */
    @GetMapping("/{id}/documents/zip")
    public void downloadAllAsZip(
            @PathVariable("id") Long id,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
        if (!identity.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required to download all documents ZIP.");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Loan_Application_" + id + "_Documents.zip\"");

        loanApplicationService.downloadAllAsZip(id, response.getOutputStream(), identity);
        response.flushBuffer();
    }

    /**
     * Admin: Generate combined PDF merging all application documents.
     */
    @GetMapping("/{id}/combined-pdf")
    public ResponseEntity<?> getCombinedPdf(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            byte[] pdfBytes = loanApplicationService.generateCombinedPdf(id, identity);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);
            headers.setContentDisposition(ContentDisposition.attachment().filename("Loan_Application_" + id + "_Combined_Documents.pdf").build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to generate combined PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to generate combined PDF: " + e.getMessage()));
        }
    }

    /**
     * Admin: Generate application summary PDF.
     */
    @GetMapping("/{id}/summary-pdf")
    public ResponseEntity<?> getSummaryPdf(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            LoanAuthService.CurrentIdentity identity = loanAuthService.resolveIdentity(request);
            byte[] pdfBytes = loanApplicationService.generateSummaryPdf(id, identity);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);
            headers.setContentDisposition(ContentDisposition.attachment().filename("Loan_Application_" + id + "_Summary.pdf").build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to generate summary PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to generate summary PDF: " + e.getMessage()));
        }
    }
}
