package com.project.kfpcl_exports.loan.service;

import com.project.kfpcl_exports.loan.model.LoanApplication;
import com.project.kfpcl_exports.loan.model.LoanDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LoanPdfService {

    private static final Logger log = LoggerFactory.getLogger(LoanPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    /**
     * Generate an Application Summary PDF using Apache PDFBox.
     */
    public byte[] generateSummaryPdf(LoanApplication app) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            float margin = 40;
            float yStart = 800;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // Header Banner
                content.setNonStrokingColor(0.12f, 0.45f, 0.25f); // Deep forest green
                content.addRect(margin, yStart - 45, 515, 45);
                content.fill();

                content.beginText();
                content.setNonStrokingColor(1.0f, 1.0f, 1.0f);
                content.setFont(fontBold, 15);
                content.newLineAtOffset(margin + 15, yStart - 25);
                content.showText("KFPCL AGRICULTURAL LOAN APPLICATION");
                content.endText();

                content.beginText();
                content.setFont(fontRegular, 9);
                content.newLineAtOffset(margin + 15, yStart - 38);
                content.showText("Krishi Farmer Producer Company Limited - Official Document Summary");
                content.endText();

                float y = yStart - 65;

                // Application Reference & Status Box
                content.setNonStrokingColor(0.95f, 0.97f, 0.95f);
                content.addRect(margin, y - 35, 515, 35);
                content.fill();

                content.setStrokingColor(0.8f, 0.85f, 0.8f);
                content.addRect(margin, y - 35, 515, 35);
                content.stroke();

                content.beginText();
                content.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                content.setFont(fontBold, 10);
                content.newLineAtOffset(margin + 10, y - 18);
                content.showText("Application No: " + safe(app.getApplicationNumber()));
                content.setFont(fontRegular, 10);
                content.newLineAtOffset(240, 0);
                content.showText("Date: " + (app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_FORMATTER) : "N/A"));
                content.endText();

                content.beginText();
                content.setFont(fontBold, 10);
                content.newLineAtOffset(margin + 10, y - 30);
                content.showText("Status: " + safe(app.getStatus()));
                content.setFont(fontRegular, 10);
                content.newLineAtOffset(240, 0);
                content.showText("Customer ID: " + safe(app.getCustomerId()));
                content.endText();

                y -= 50;

                // Section 1: Applicant Details
                drawSectionHeader(content, fontBold, margin, y, "1. APPLICANT DETAILS");
                y -= 20;

                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Applicant Name:", safe(app.getApplicantName()));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Mobile Number:", safe(app.getPhoneNumber()));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Email Address:", safe(app.getEmail()));

                y -= 10;

                // Section 2: Loan & Farm Details
                drawSectionHeader(content, fontBold, margin, y, "2. LOAN & AGRICULTURAL DETAILS");
                y -= 20;

                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Requested Loan Amount:", "INR " + (app.getLoanAmount() != null ? String.format("%,.2f", app.getLoanAmount()) : "0.00"));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Loan Purpose:", safe(app.getLoanPurpose()));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Annual Income:", "INR " + (app.getAnnualIncome() != null ? String.format("%,.2f", app.getAnnualIncome()) : "0.00"));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Land Holding:", (app.getLandSizeAcres() != null ? app.getLandSizeAcres() + " Acres" : "N/A"));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Crop / Cultivation Type:", safe(app.getCropType()));
                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Farm Location:", safe(app.getFarmLocation()));

                y -= 10;

                // Section 3: Document Verification Checklist
                drawSectionHeader(content, fontBold, margin, y, "3. UPLOADED DOCUMENTS CHECKLIST (SUPABASE S3 STORAGE)");
                y -= 20;

                // Table Header
                content.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                content.beginText();
                content.setFont(fontBold, 8);
                content.newLineAtOffset(margin + 5, y);
                content.showText("CATEGORY");
                content.newLineAtOffset(90, 0);
                content.showText("FILE NAME");
                content.newLineAtOffset(160, 0);
                content.showText("SIZE");
                content.newLineAtOffset(60, 0);
                content.showText("UPLOAD TIME");
                content.newLineAtOffset(110, 0);
                content.showText("STATUS");
                content.endText();

                y -= 6;
                content.setStrokingColor(0.7f, 0.7f, 0.7f);
                content.moveTo(margin, y);
                content.lineTo(margin + 515, y);
                content.stroke();
                y -= 12;

                List<LoanDocument> docs = app.getDocuments();
                if (docs != null && !docs.isEmpty()) {
                    for (LoanDocument doc : docs) {
                        content.beginText();
                        content.setFont(fontRegular, 8);
                        content.newLineAtOffset(margin + 5, y);
                        content.showText(truncate(safe(doc.getDocumentCategory()), 18));
                        content.newLineAtOffset(90, 0);
                        content.showText(truncate(safe(doc.getOriginalFilename()), 30));
                        content.newLineAtOffset(160, 0);
                        content.showText((doc.getFileSize() != null ? (doc.getFileSize() / 1024) + " KB" : "-"));
                        content.newLineAtOffset(60, 0);
                        content.showText(doc.getUploadTimestamp() != null ? doc.getUploadTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) : "-");
                        content.newLineAtOffset(110, 0);
                        content.showText(safe(doc.getUploadStatus()));
                        content.endText();

                        y -= 14;
                    }
                } else {
                    content.beginText();
                    content.setFont(fontOblique, 8);
                    content.newLineAtOffset(margin + 10, y);
                    content.showText("No documents recorded.");
                    content.endText();
                    y -= 14;
                }

                y -= 10;

                // Section 4: Admin Review Notes
                drawSectionHeader(content, fontBold, margin, y, "4. ADMINISTRATIVE REVIEW & VERIFICATION");
                y -= 20;

                y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Current Status:", safe(app.getStatus()));
                if (app.getAdminNotes() != null && !app.getAdminNotes().isBlank()) {
                    y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Admin Notes:", safe(app.getAdminNotes()));
                }
                if (app.getRejectionReason() != null && !app.getRejectionReason().isBlank()) {
                    y = drawKeyValue(content, fontBold, fontRegular, margin + 10, y, "Rejection Reason:", safe(app.getRejectionReason()));
                }

                // Footer
                content.setFont(fontOblique, 8);
                content.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                content.beginText();
                content.newLineAtOffset(margin, 35);
                content.showText("This is a computer-generated summary document from KFPCL Backend. Retrieved from private Supabase S3 bucket.");
                content.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Merge multiple supported documents (PDFs and Images) into a single combined PDF.
     */
    public byte[] generateCombinedPdf(LoanApplication app, List<LoanDocumentData> documents) throws IOException {
        try (PDDocument combinedDoc = new PDDocument()) {
            // First page: Add Application Summary cover sheet
            byte[] summaryBytes = generateSummaryPdf(app);
            try (PDDocument summaryDoc = Loader.loadPDF(summaryBytes)) {
                for (int i = 0; i < summaryDoc.getNumberOfPages(); i++) {
                    combinedDoc.importPage(summaryDoc.getPage(i));
                }
            }

            // Append each document
            for (LoanDocumentData item : documents) {
                byte[] data = item.data();
                String contentType = item.contentType() != null ? item.contentType().toLowerCase() : "";
                String category = item.category();

                if (data == null || data.length == 0) continue;

                if (contentType.contains("pdf")) {
                    try (PDDocument childPdf = Loader.loadPDF(data)) {
                        for (int i = 0; i < childPdf.getNumberOfPages(); i++) {
                            combinedDoc.importPage(childPdf.getPage(i));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to append PDF document for category '{}': {}", category, e.getMessage());
                    }
                } else if (contentType.contains("image") || isImage(data)) {
                    try {
                        PDPage imagePage = new PDPage(PDRectangle.A4);
                        combinedDoc.addPage(imagePage);

                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(combinedDoc, data, category);
                        float pageWidth = PDRectangle.A4.getWidth();
                        float pageHeight = PDRectangle.A4.getHeight();
                        float margin = 40;
                        float maxWidth = pageWidth - (2 * margin);
                        float maxHeight = pageHeight - (2 * margin) - 40; // reserve space for header

                        float imgWidth = pdImage.getWidth();
                        float imgHeight = pdImage.getHeight();
                        float scale = Math.min(maxWidth / imgWidth, maxHeight / imgHeight);
                        if (scale > 1.0f) scale = 1.0f; // do not upscale smaller images

                        float drawWidth = imgWidth * scale;
                        float drawHeight = imgHeight * scale;
                        float x = margin + (maxWidth - drawWidth) / 2;
                        float y = margin + (maxHeight - drawHeight) / 2;

                        try (PDPageContentStream cs = new PDPageContentStream(combinedDoc, imagePage)) {
                            // Header banner on image page
                            cs.beginText();
                            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                            cs.newLineAtOffset(margin, pageHeight - margin);
                            cs.showText("DOCUMENT: " + category.toUpperCase() + " (" + safe(item.filename()) + ")");
                            cs.endText();

                            cs.drawImage(pdImage, x, y, drawWidth, drawHeight);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to render image page for category '{}': {}", category, e.getMessage());
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            combinedDoc.save(baos);
            return baos.toByteArray();
        }
    }

    public record LoanDocumentData(
            String category,
            String filename,
            String contentType,
            byte[] data
    ) {}

    private void drawSectionHeader(PDPageContentStream content, PDType1Font font, float x, float y, String title) throws IOException {
        content.setNonStrokingColor(0.12f, 0.45f, 0.25f);
        content.beginText();
        content.setFont(font, 10);
        content.newLineAtOffset(x, y);
        content.showText(title);
        content.endText();

        content.setStrokingColor(0.85f, 0.85f, 0.85f);
        content.moveTo(x, y - 4);
        content.lineTo(x + 515, y - 4);
        content.stroke();
    }

    private float drawKeyValue(PDPageContentStream content, PDType1Font boldFont, PDType1Font regularFont,
                                float x, float y, String key, String value) throws IOException {
        content.setNonStrokingColor(0.2f, 0.2f, 0.2f);
        content.beginText();
        content.setFont(boldFont, 9);
        content.newLineAtOffset(x, y);
        content.showText(key);
        content.setFont(regularFont, 9);
        content.newLineAtOffset(160, 0);
        content.showText(value);
        content.endText();
        return y - 14;
    }

    private boolean isImage(byte[] data) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            return img != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String safe(String str) {
        return str != null ? str : "N/A";
    }

    private String truncate(String str, int max) {
        if (str == null) return "";
        return str.length() > max ? str.substring(0, max - 3) + "..." : str;
    }
}
