package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.FileUploadResponse;
import com.project.kfpcl_exports.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    @Test
    @DisplayName("POST /api/media/upload should upload file and return 201 Created")
    void testUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image data".getBytes());

        FileUploadResponse response = FileUploadResponse.builder()
                .key("media/products/uuid-test.jpg")
                .url("/api/media/view?key=media/products/uuid-test.jpg")
                .presignedUrl("https://s3.amazonaws.com/test")
                .fileName("test.jpg")
                .fileSize(10L)
                .contentType("image/jpeg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(s3Service.uploadImage(any(), eq("products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("media/products/uuid-test.jpg"))
                .andExpect(jsonPath("$.url").value("/api/media/view?key=media/products/uuid-test.jpg"))
                .andExpect(jsonPath("$.presignedUrl").value("https://s3.amazonaws.com/test"));
    }

    @Test
    @DisplayName("POST /api/media/upload with validation error should return 400 Bad Request")
    void testUploadValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "pdf data".getBytes());

        when(s3Service.uploadImage(any(), any())).thenThrow(new IllegalArgumentException("Invalid file type"));

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid file type"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/media/view should stream image bytes with caching headers")
    void testViewFile() throws Exception {
        byte[] imageBytes = "dummy image content".getBytes();
        GetObjectResponse getObjectResponse = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .contentLength((long) imageBytes.length)
                .build();

        ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                getObjectResponse,
                AbortableInputStream.create(new ByteArrayInputStream(imageBytes))
        );

        when(s3Service.getObjectStream("media/test.jpg")).thenReturn(responseInputStream);
        when(s3Service.extractObjectKey("media/test.jpg")).thenReturn("media/test.jpg");

        mockMvc.perform(get("/api/media/view").param("key", "media/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    @DisplayName("GET /api/media/view should return 404 if object not found in S3")
    void testViewFileNotFound() throws Exception {
        when(s3Service.getObjectStream("missing.jpg")).thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        mockMvc.perform(get("/api/media/view").param("key", "missing.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found in S3"));
    }

    @Test
    @DisplayName("GET /api/media/presigned-url should return presigned URL")
    void testGetPresignedUrl() throws Exception {
        when(s3Service.generatePresignedGetUrl(eq("media/test.jpg"), anyInt()))
                .thenReturn("https://s3.ap-south-2.amazonaws.com/presigned-url-test");
        when(s3Service.extractObjectKey("media/test.jpg")).thenReturn("media/test.jpg");

        mockMvc.perform(get("/api/media/presigned-url").param("key", "media/test.jpg").param("expiryMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("media/test.jpg"))
                .andExpect(jsonPath("$.presignedUrl").value("https://s3.ap-south-2.amazonaws.com/presigned-url-test"))
                .andExpect(jsonPath("$.expiryMinutes").value(30));
    }

    @Test
    @DisplayName("DELETE /api/media/delete should delete object and return 200")
    void testDeleteFile() throws Exception {
        doNothing().when(s3Service).deleteObject("media/test.jpg");
        when(s3Service.extractObjectKey("media/test.jpg")).thenReturn("media/test.jpg");

        mockMvc.perform(delete("/api/media/delete").param("key", "media/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.key").value("media/test.jpg"));
    }
}
