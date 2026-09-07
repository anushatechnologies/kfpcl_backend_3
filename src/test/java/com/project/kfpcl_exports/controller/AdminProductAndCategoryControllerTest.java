package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Category;
import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.admin.model.Subcategory;
import com.project.kfpcl_exports.admin.repository.CategoryRepository;
import com.project.kfpcl_exports.admin.repository.ProductImageRepository;
import com.project.kfpcl_exports.admin.repository.ProductRepository;
import com.project.kfpcl_exports.admin.repository.SubcategoryRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(locations = "classpath:application-test.properties")
public class AdminProductAndCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @MockBean
    private S3Service s3Service;

    @Test
    @DisplayName("POST /api/categories with multipart image should upload to S3 and save category with imageUrl")
    void testCreateCategoryMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "spices.jpg", "image/jpeg", "fake image".getBytes());

        FileUploadResponse uploadRes = FileUploadResponse.builder()
                .key("media/categories/uuid-spices.jpg")
                .url("/api/media/view?key=media/categories/uuid-spices.jpg")
                .presignedUrl("https://s3.amazonaws.com/test-cat")
                .fileName("spices.jpg")
                .fileSize(100L)
                .contentType("image/jpeg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(s3Service.uploadImage(any(), eq("categories"))).thenReturn(uploadRes);

        mockMvc.perform(multipart("/api/categories")
                        .file(file)
                        .param("name", "Spices")
                        .param("description", "Indian Spices")
                        .param("discount", "5.0")
                        .param("active", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spices"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/view?key=media/categories/uuid-spices.jpg"))
                .andExpect(jsonPath("$.image").value("/api/media/view?key=media/categories/uuid-spices.jpg"));
    }

    @Test
    @DisplayName("POST /api/categories with JSON containing 'image' alias should save category with imageUrl")
    void testCreateCategoryJsonWithImage() throws Exception {
        String json = "{\"name\":\"Grains\",\"description\":\"Pulses & Grains\",\"image\":\"/api/media/view?key=grains.jpg\"}";

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grains"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/view?key=grains.jpg"))
                .andExpect(jsonPath("$.image").value("/api/media/view?key=grains.jpg"));
    }

    @Test
    @DisplayName("POST /api/subcategories with multipart image should upload to S3 and save subcategory with imageUrl")
    void testCreateSubcategoryMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cardamom.jpg", "image/jpeg", "fake image".getBytes());

        FileUploadResponse uploadRes = FileUploadResponse.builder()
                .key("media/subcategories/uuid-cardamom.jpg")
                .url("/api/media/view?key=media/subcategories/uuid-cardamom.jpg")
                .presignedUrl("https://s3.amazonaws.com/test-sub")
                .fileName("cardamom.jpg")
                .fileSize(100L)
                .contentType("image/jpeg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(s3Service.uploadImage(any(), eq("subcategories"))).thenReturn(uploadRes);

        mockMvc.perform(multipart("/api/subcategories")
                        .file(file)
                        .param("name", "Cardamom")
                        .param("description", "Green Cardamom")
                        .param("categoryId", "1")
                        .param("categoryName", "Spices"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cardamom"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/view?key=media/subcategories/uuid-cardamom.jpg"))
                .andExpect(jsonPath("$.image").value("/api/media/view?key=media/subcategories/uuid-cardamom.jpg"));
    }

    @Test
    @DisplayName("POST /api/products with multipart image should upload to S3 and save product with mainImageUrl and imageUrl")
    void testCreateProductMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "turmeric.jpg", "image/jpeg", "fake image".getBytes());

        FileUploadResponse uploadRes = FileUploadResponse.builder()
                .key("media/products/uuid-turmeric.jpg")
                .url("/api/media/view?key=media/products/uuid-turmeric.jpg")
                .presignedUrl("https://s3.amazonaws.com/test-prod")
                .fileName("turmeric.jpg")
                .fileSize(200L)
                .contentType("image/jpeg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(s3Service.uploadImage(any(), eq("products"))).thenReturn(uploadRes);

        mockMvc.perform(multipart("/api/products")
                        .file(file)
                        .param("title", "Organic Turmeric Powder")
                        .param("description", "High Curcumin Turmeric")
                        .param("price", "250.0")
                        .param("stock", "50")
                        .param("unit", "kg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Organic Turmeric Powder"))
                .andExpect(jsonPath("$.mainImageUrl").value("/api/media/view?key=media/products/uuid-turmeric.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/view?key=media/products/uuid-turmeric.jpg"))
                .andExpect(jsonPath("$.image").value("/api/media/view?key=media/products/uuid-turmeric.jpg"))
                .andExpect(jsonPath("$.images[0].imageUrl").value("/api/media/view?key=media/products/uuid-turmeric.jpg"));
    }

    @Test
    @DisplayName("POST /api/products with JSON containing 'imageUrl' should map to mainImageUrl and imageUrl")
    void testCreateProductJsonWithImageUrl() throws Exception {
        String json = "{\"title\":\"Black Pepper\",\"price\":500.0,\"imageUrl\":\"/api/media/view?key=pepper.jpg\"}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Black Pepper"))
                .andExpect(jsonPath("$.mainImageUrl").value("/api/media/view?key=pepper.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("/api/media/view?key=pepper.jpg"))
                .andExpect(jsonPath("$.image").value("/api/media/view?key=pepper.jpg"));
    }
}
