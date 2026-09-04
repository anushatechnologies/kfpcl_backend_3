package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.config.AwsS3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private AwsS3Config awsS3Config;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // Mock default behavior if needed
    }

    @Test
    @DisplayName("Should reject empty file")
    void testUploadEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadImage(emptyFile, "products"));
        assertTrue(ex.getMessage().contains("Cannot upload an empty file"));
    }

    @Test
    @DisplayName("Should reject non-image file type")
    void testUploadInvalidContentType() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", "dummy content".getBytes());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadImage(pdfFile, "products"));
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    @DisplayName("Should reject file exceeding 10MB limit")
    void testUploadOversizedFile() {
        byte[] oversizedBytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile bigFile = new MockMultipartFile("file", "huge.jpg", "image/jpeg", oversizedBytes);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadImage(bigFile, "products"));
        assertTrue(ex.getMessage().contains("File size exceeds 10 MB limit"));
    }

    @Test
    @DisplayName("Should extract raw object key from different URL formats")
    void testExtractObjectKey() {
        // Direct key
        assertEquals("media/products/item-1.jpg", s3Service.extractObjectKey("media/products/item-1.jpg"));

        // Leading slash
        assertEquals("media/products/item-1.jpg", s3Service.extractObjectKey("/media/products/item-1.jpg"));

        // Backend view URL
        assertEquals("media/products/item-1.jpg", s3Service.extractObjectKey("/api/media/view?key=media/products/item-1.jpg"));

        // URL encoded backend view URL
        assertEquals("media/products/item-1.jpg", s3Service.extractObjectKey("/api/media/view?key=media%2Fproducts%2Fitem-1.jpg"));

        // Full S3 URL
        assertEquals("media/products/item-1.jpg", s3Service.extractObjectKey("https://kfpcl-exports-media-319759856065.s3.ap-south-2.amazonaws.com/media/products/item-1.jpg"));
    }
}
