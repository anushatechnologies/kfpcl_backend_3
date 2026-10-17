package com.project.kfpcl_exports.loan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.config.AwsS3Config;
import com.project.kfpcl_exports.loan.model.LoanApplication;
import com.project.kfpcl_exports.loan.model.LoanDocument;
import com.project.kfpcl_exports.loan.repository.LoanApplicationRepository;
import com.project.kfpcl_exports.loan.repository.LoanDocumentRepository;
import com.project.kfpcl_exports.loan.service.LoanDocumentStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:loan_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
public class LoanApplicationWorkflowLiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private AwsS3Config awsS3Config;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LoanDocumentRepository loanDocumentRepository;

    private final List<String> createdS3Keys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdS3Keys.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up any uploaded test objects from Supabase bucket
        String bucket = awsS3Config.getS3Bucket();
        for (String key : createdS3Keys) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
                System.out.println("Cleaned up Supabase S3 test object: " + key);
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Complete Workflow: Submit Loan Application, Verify Supabase S3 Objects, Access Control, and Admin Downloads")
    void testCompleteLoanApplicationWorkflow() throws Exception {
        String bucket = awsS3Config.getS3Bucket();
        assertNotNull(bucket, "Supabase S3 bucket must be configured");

        String applicantPhone = "9988776655";
        String customerId = "CUST-9988";

        // Minimal 1x1 valid JPEG bytes for image upload test
        byte[] sampleJpgBytes = new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
                0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x60, 0x00, 0x60, 0x00, 0x00,
                (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
                0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34,
                0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32, (byte) 0xFF,
                (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00,
                (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01, 0x01,
                0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02,
                0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, (byte) 0xFF, (byte) 0xDA,
                0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00, 0x7F, 0x00, (byte) 0xFF, (byte) 0xD9
        };

        // Minimal valid PDF document bytes
        byte[] samplePdfBytes = ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\n" +
                "xref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000115 00000 n\n" +
                "trailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);

        MockMultipartFile aadhaarFile = new MockMultipartFile("aadhaar", "aadhaar_test.pdf", "application/pdf", samplePdfBytes);
        MockMultipartFile panFile = new MockMultipartFile("pan", "pan_test.pdf", "application/pdf", samplePdfBytes);
        MockMultipartFile photoFile = new MockMultipartFile("photo", "photo_test.jpg", "image/jpeg", sampleJpgBytes);
        MockMultipartFile passbookFile = new MockMultipartFile("bankPassbook", "passbook_test.pdf", "application/pdf", samplePdfBytes);
        MockMultipartFile landFile = new MockMultipartFile("landRecords", "land_test.pdf", "application/pdf", samplePdfBytes);
        MockMultipartFile cropFile = new MockMultipartFile("cropDetails", "crop_test.pdf", "application/pdf", samplePdfBytes);
        MockMultipartFile leaseFile = new MockMultipartFile("leaseDocuments", "lease_test.pdf", "application/pdf", samplePdfBytes);

        // 1. Submit Loan Application via POST /api/loan-applications
        MvcResult submitResult = mockMvc.perform(multipart("/api/loan-applications")
                        .file(aadhaarFile)
                        .file(panFile)
                        .file(photoFile)
                        .file(passbookFile)
                        .file(landFile)
                        .file(cropFile)
                        .file(leaseFile)
                        .param("applicantName", "Ramesh Patel")
                        .param("phoneNumber", applicantPhone)
                        .param("email", "ramesh.patel@agri.com")
                        .param("loanAmount", "250000.00")
                        .param("loanPurpose", "Solar Irrigation Pump")
                        .param("annualIncome", "450000.00")
                        .param("landSizeAcres", "5.5")
                        .param("cropType", "Cotton and Pulses")
                        .param("farmLocation", "Wardha, Maharashtra")
                        .header("X-Customer-Id", customerId)
                        .header("X-Phone-Number", applicantPhone))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.applicationNumber").exists())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn();

        JsonNode root = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        Long applicationId = root.path("data").path("id").asLong();
        String applicationNumber = root.path("data").path("applicationNumber").asText();
        JsonNode docsNode = root.path("data").path("documents");

        assertTrue(docsNode.isArray());
        assertEquals(7, docsNode.size(), "All 7 documents should be uploaded and recorded");

        // 2. Verify files actually appear in Supabase Storage with correct object key patterns
        for (JsonNode docItem : docsNode) {
            String category = docItem.path("documentCategory").asText();
            String objectKey = docItem.path("objectKey").asText();
            String uploadStatus = docItem.path("uploadStatus").asText();

            assertEquals("UPLOADED", uploadStatus);
            createdS3Keys.add(objectKey);

            // Pattern: loan-applications/{applicationId}/{category}/{uniqueFileId}.{ext}
            assertTrue(objectKey.startsWith("loan-applications/" + applicationNumber + "/" + category + "/"),
                    "Key must follow expected pattern: " + objectKey);

            // Verify the object physically exists in Supabase bucket!
            HeadObjectRequest headReq = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            HeadObjectResponse headRes = s3Client.headObject(headReq);
            assertNotNull(headRes, "Object must exist in Supabase Storage: " + objectKey);
            assertTrue(headRes.contentLength() > 0, "Uploaded object size must be > 0 in Supabase Storage");
        }

        // 3. Verify database references correct object keys and metadata
        List<LoanDocument> dbDocs = loanDocumentRepository.findByLoanApplicationId(applicationId);
        assertEquals(7, dbDocs.size());
        for (LoanDocument dbDoc : dbDocs) {
            assertTrue(createdS3Keys.contains(dbDoc.getObjectKey()));
            assertEquals("UPLOADED", dbDoc.getUploadStatus());
            assertNotNull(dbDoc.getContentType());
            assertNotNull(dbDoc.getUploadTimestamp());
        }

        LoanDocument aadhaarDoc = dbDocs.stream()
                .filter(d -> d.getDocumentCategory().equals("aadhaar"))
                .findFirst().orElseThrow();

        // 4. Verify Unauthorized access control: another customer cannot access
        mockMvc.perform(get("/api/loan-applications/" + applicationId)
                        .header("X-Customer-Id", "OTHER-HACKER-ID")
                        .header("X-Phone-Number", "1111111111"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/loan-applications/" + applicationId + "/documents/" + aadhaarDoc.getId() + "/download")
                        .header("X-Customer-Id", "OTHER-HACKER-ID")
                        .header("X-Phone-Number", "1111111111"))
                .andExpect(status().isForbidden());

        // 5. Verify Owner Customer CAN access
        mockMvc.perform(get("/api/loan-applications/" + applicationId)
                        .header("X-Customer-Id", customerId)
                        .header("X-Phone-Number", applicantPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(applicationId));

        mockMvc.perform(get("/api/loan-applications/" + applicationId + "/documents/" + aadhaarDoc.getId() + "/download")
                        .header("X-Customer-Id", customerId)
                        .header("X-Phone-Number", applicantPhone))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE));

        // 6. Verify Owner Customer can get short-lived presigned URL
        mockMvc.perform(get("/api/loan-applications/" + applicationId + "/documents/" + aadhaarDoc.getId() + "/presigned-url")
                        .header("X-Customer-Id", customerId)
                        .header("X-Phone-Number", applicantPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").exists());

        // 7. Verify Admin Downloads
        // 7a. Admin download individual document
        mockMvc.perform(get("/api/admin/loan-applications/" + applicationId + "/documents/" + aadhaarDoc.getId() + "/download")
                        .header("X-Admin-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE));

        // 7b. Admin presigned URL
        mockMvc.perform(get("/api/admin/loan-applications/" + applicationId + "/documents/" + aadhaarDoc.getId() + "/presigned-url")
                        .header("X-Admin-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").exists());

        // 7c. Admin download all documents as ZIP
        MvcResult zipResult = mockMvc.perform(get("/api/admin/loan-applications/" + applicationId + "/documents/zip")
                        .header("X-Admin-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn();

        byte[] zipBytes = zipResult.getResponse().getContentAsByteArray();
        assertTrue(zipBytes.length > 0, "ZIP bytes must not be empty");

        // Verify ZIP contents
        List<String> zipEntryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zipEntryNames.add(entry.getName());
                zis.closeEntry();
            }
        }
        assertTrue(zipEntryNames.stream().anyMatch(name -> name.contains("Application_Summary")), "ZIP must include application summary");
        assertTrue(zipEntryNames.stream().anyMatch(name -> name.contains("aadhaar")), "ZIP must include aadhaar document");
        assertTrue(zipEntryNames.stream().anyMatch(name -> name.contains("pan")), "ZIP must include pan document");
        assertTrue(zipEntryNames.stream().anyMatch(name -> name.contains("photo")), "ZIP must include photo document");

        // 7d. Admin generate combined PDF
        MvcResult combinedPdfResult = mockMvc.perform(get("/api/admin/loan-applications/" + applicationId + "/combined-pdf")
                        .header("X-Admin-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        byte[] combinedPdfBytes = combinedPdfResult.getResponse().getContentAsByteArray();
        assertTrue(combinedPdfBytes.length > 100, "Combined PDF must be generated");
        assertEquals("%PDF-", new String(combinedPdfBytes, 0, 5, StandardCharsets.US_ASCII), "Must be valid PDF");

        // 7e. Admin generate Application Summary PDF
        MvcResult summaryPdfResult = mockMvc.perform(get("/api/admin/loan-applications/" + applicationId + "/summary-pdf")
                        .header("X-Admin-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        byte[] summaryPdfBytes = summaryPdfResult.getResponse().getContentAsByteArray();
        assertTrue(summaryPdfBytes.length > 100, "Summary PDF must be generated");
        assertEquals("%PDF-", new String(summaryPdfBytes, 0, 5, StandardCharsets.US_ASCII), "Must be valid PDF");

        // 7f. Admin update status
        mockMvc.perform(put("/api/admin/loan-applications/" + applicationId + "/status")
                        .header("X-Admin-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"adminNotes\":\"Verified all land and KYC documents in Supabase bucket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Partial Failure Safety: Missing required files or upload failure never produces false-success submission")
    void testPartialFailureDoesNotProduceSuccess() throws Exception {
        byte[] samplePdf = "%PDF-1.4 dummy".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile aadhaarFile = new MockMultipartFile("aadhaar", "aadhaar.pdf", "application/pdf", samplePdf);
        MockMultipartFile panFile = new MockMultipartFile("pan", "pan.pdf", "application/pdf", samplePdf);

        // Missing bankPassbook, photo, landRecords!
        mockMvc.perform(multipart("/api/loan-applications")
                        .file(aadhaarFile)
                        .file(panFile)
                        .param("applicantName", "Incomplete Applicant")
                        .param("phoneNumber", "9876543210")
                        .param("loanAmount", "50000.00")
                        .param("loanPurpose", "Seeds Purchase"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Missing required loan documents")));

        // Verify database has no orphaned loan application
        assertEquals(0, loanApplicationRepository.findByPhoneNumberOrderByCreatedAtDesc("9876543210").size());
    }
}
