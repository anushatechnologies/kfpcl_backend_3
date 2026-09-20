package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.dto.ProductRequestDTO;
import com.project.kfpcl_exports.admin.dto.ProductVariantDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminProductVariantTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create Product with Variants and Retrieve with Variants")
    void testCreateAndGetProductWithVariants() throws Exception {
        ProductVariantDTO v1 = ProductVariantDTO.builder()
                .name("500g")
                .sku("TD-500G")
                .price(120.00)
                .discountPrice(110.00)
                .stock(50)
                .isActive(true)
                .displayOrder(1)
                .build();

        ProductVariantDTO v2 = ProductVariantDTO.builder()
                .name("1kg")
                .sku("TD-1KG")
                .price(230.00)
                .discountPrice(210.00)
                .stock(30)
                .isActive(true)
                .displayOrder(2)
                .build();

        ProductRequestDTO request = ProductRequestDTO.builder()
                .title("Test Toor Dal With Variants")
                .description("Premium Dal")
                .price(120.00)
                .stock(80)
                .unit("kg")
                .variants(List.of(v1, v2))
                .build();

        String responseJson = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].name", is("500g")))
                .andExpect(jsonPath("$.variants[1].name", is("1kg")))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(get("/api/admin/products/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].sku", is("TD-500G")))
                .andExpect(jsonPath("$.variants[1].sku", is("TD-1KG")));

        // Test Buyer API endpoint returns the variants with variantName field
        mockMvc.perform(get("/api/buyer/products/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].name", is("500g")))
                .andExpect(jsonPath("$.variants[0].variantName", is("500g")))
                .andExpect(jsonPath("$.variants[1].name", is("1kg")))
                .andExpect(jsonPath("$.variants[1].variantName", is("1kg")));
    }
}
