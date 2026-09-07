package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.dto.QuotationRequest;
import com.project.kfpcl_exports.buyer.dto.BuyerCreateRfqRequest;
import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class RfqLifecycleE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository buyerProductRepository;

    @Test
    @DisplayName("Complete Enquiry to Supplier (RFQ) Lifecycle: Create -> Supplier Feed -> Quote -> Buyer View -> Accept -> Contact Exchange")
    void testCompleteRfqLifecycle() throws Exception {
        // 0. Seed Product
        Product product = new Product();
        product.setName("Srilalitha Premium Idly Rava");
        product.setNumericPrice(new BigDecimal("50.00"));
        product.setIsActive(true);
        Product savedProduct = buyerProductRepository.save(product);

        // =========================================================================
        // Step 1: Buyer Creates Enquiry (POST /api/buyer/rfqs)
        // =========================================================================
        BuyerCreateRfqRequest createReq = BuyerCreateRfqRequest.builder()
                .productId(savedProduct.getId())
                .buyerName("Rahul Sharma")
                .buyerPhone("9876543210")
                .quantity("500 KG")
                .deliveryLocation("APMC Vashi, Navi Mumbai")
                .subject("Bulk price for export grade Srilalitha Idly Rava")
                .buyerMessage("Buyer Name: Rahul Sharma\nBuyer Mobile: +91 9876543210\nSubject: Bulk price for export grade Srilalitha Idly Rava\nNeed test report & export packing specs.")
                .email("rahul.sharma@example.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/buyer/rfqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.buyerName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.data.buyerPhone").value("9876543210"))
                .andExpect(jsonPath("$.data.title").value("Srilalitha Premium Idly Rava"))
                .andExpect(jsonPath("$.data.quantity").value("500 KG"))
                .andExpect(jsonPath("$.data.deliveryLocation").value("APMC Vashi, Navi Mumbai"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String rfqCode = rootNode.path("data").path("rfqCode").asText();
        assertNotNull(rfqCode, "RFQ code must be generated");

        // =========================================================================
        // Step 2: Supplier / Admin RFQ Feed (GET /api/admin/rfqs)
        // =========================================================================
        mockMvc.perform(get("/api/admin/rfqs?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rfqs").isArray())
                .andExpect(jsonPath("$.data.totalPages").exists());

        // =========================================================================
        // Step 3: Supplier Submits Quotation (POST /api/admin/rfqs/{rfqId}/quotation)
        // =========================================================================
        QuotationRequest quoteReq = new QuotationRequest();
        quoteReq.setUnitPrice(48.50);
        quoteReq.setQuantity(500);
        quoteReq.setDeliveryDays("5 days");
        quoteReq.setNotes("Price includes GST & loading at warehouse. FSSAI report attached.");

        mockMvc.perform(post("/api/admin/rfqs/" + rfqCode + "/quotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rfqCode").value(rfqCode))
                .andExpect(jsonPath("$.data.unitPrice").value(48.50))
                .andExpect(jsonPath("$.data.totalAmount").value(24250.00))
                .andExpect(jsonPath("$.data.leadTime").value("5 days"))
                .andExpect(jsonPath("$.data.status").value("QUOTED"));

        // =========================================================================
        // Step 4: Buyer Views Enquiry and Received Quotation (GET /api/buyer/rfqs/{rfqCode})
        // =========================================================================
        mockMvc.perform(get("/api/buyer/rfqs/" + rfqCode)
                        .header("X-Phone-Number", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESPONDED"))
                .andExpect(jsonPath("$.data.response.offeredPrice").value(48.50))
                .andExpect(jsonPath("$.data.response.totalAmount").value(24250.00))
                .andExpect(jsonPath("$.data.response.leadTime").value("5 days"));

        // =========================================================================
        // Step 5: Buyer Accepts Quotation (POST /api/buyer/rfqs/{rfqCode}/accept)
        // =========================================================================
        mockMvc.perform(post("/api/buyer/rfqs/" + rfqCode + "/accept")
                        .header("X-Phone-Number", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // =========================================================================
        // Step 6: Buyer Retrieves Supplier Contact Details (GET /api/buyer/rfqs/{rfqCode}/contact)
        // =========================================================================
        mockMvc.perform(get("/api/buyer/rfqs/" + rfqCode + "/contact")
                        .header("X-Phone-Number", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supplierName").value("KFPCL Farmer Producer Co."))
                .andExpect(jsonPath("$.data.contactPerson").exists())
                .andExpect(jsonPath("$.data.phone").exists())
                .andExpect(jsonPath("$.data.email").exists());
    }
}
