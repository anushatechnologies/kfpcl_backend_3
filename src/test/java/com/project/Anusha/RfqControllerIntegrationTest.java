package com.project.Anusha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.Anusha.dto.*;
import com.project.Anusha.model.*;
import com.project.Anusha.repository.*;
import com.project.Anusha.service.RfqService;
import com.project.Anusha.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RfqControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RfqRepository rfqRepository;

    @Autowired
    private RfqService rfqService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User buyerA;
    private User buyerB;
    private String tokenA;
    private String tokenB;
    private Product product;

    @BeforeEach
    void setUp() {
        buyerA = userRepository.save(User.builder().email("buyer1@test.com").password("pass").name("Buyer One").role("ROLE_USER").build());
        buyerB = userRepository.save(User.builder().email("buyer2@test.com").password("pass").name("Buyer Two").role("ROLE_USER").build());

        tokenA = "Bearer " + jwtTokenProvider.generateToken(buyerA.getEmail(), buyerA.getRole(), buyerA.getId());
        tokenB = "Bearer " + jwtTokenProvider.generateToken(buyerB.getEmail(), buyerB.getRole(), buyerB.getId());

        Category category = categoryRepository.save(Category.builder().name("Pulses").description("High protein pulses").build());
        product = productRepository.save(Product.builder().name("Red Gram").description("Organic Red Gram").category(category).isActive(true).build());
    }

    @Test
    @DisplayName("API Test: POST /api/buyer/rfqs creates RFQ with 201 Created")
    void testCreateRfqApi() throws Exception {
        BuyerCreateRfqRequest req = BuyerCreateRfqRequest.builder()
                .productId(product.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Need quotation")
                .build();

        mockMvc.perform(post("/api/buyer/rfqs")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rfqCode", startsWith("RFQ-")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.contactAvailable", is(false)));
    }

    @Test
    @DisplayName("API Test: GET /api/buyer/rfqs returns only authenticated buyer's RFQs")
    void testGetBuyerRfqsApi() throws Exception {
        // Buyer A creates RFQ
        BuyerCreateRfqRequest reqA = BuyerCreateRfqRequest.builder()
                .productId(product.getId())
                .quantity("500 KG")
                .deliveryLocation("Vijayawada")
                .build();

        mockMvc.perform(post("/api/buyer/rfqs")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated());

        // Buyer B creates RFQ
        BuyerCreateRfqRequest reqB = BuyerCreateRfqRequest.builder()
                .productId(product.getId())
                .quantity("800 KG")
                .deliveryLocation("Guntur")
                .build();

        mockMvc.perform(post("/api/buyer/rfqs")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated());

        // Buyer A fetches - should see only 1 RFQ (500 KG)
        mockMvc.perform(get("/api/buyer/rfqs")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].quantity", is("500 KG")));
    }

    @Test
    @DisplayName("API Test: Complete buyer lifecycle - Create -> Responded -> Accept -> Get Contact")
    void testCompleteRfqLifecycleApi() throws Exception {
        // 1. Create RFQ
        BuyerCreateRfqRequest createReq = BuyerCreateRfqRequest.builder()
                .productId(product.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Bulk purchase")
                .build();

        String createResponse = mockMvc.perform(post("/api/buyer/rfqs")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String rfqCode = objectMapper.readTree(createResponse).path("data").path("rfqCode").asText();

        // 2. Simulate Admin Response via dummy data
        Rfq rfqEntity = rfqRepository.findByRfqCode(rfqCode).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Price includes transport", "KFPCL Manager", "9876543210", "manager@kfpcl.com");

        // 3. Buyer checks detail before acceptance - contact is NOT available
        mockMvc.perform(get("/api/buyer/rfqs/" + rfqCode)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESPONDED")))
                .andExpect(jsonPath("$.data.contactAvailable", is(false)))
                .andExpect(jsonPath("$.data.response.quotedPrice", is(62000.0)));

        // 4. Buyer tries to get contact before accept -> 403 Forbidden
        mockMvc.perform(get("/api/buyer/rfqs/" + rfqCode + "/contact")
                        .header("Authorization", tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("CONTACT_NOT_AVAILABLE")));

        // 5. Buyer Accepts response
        mockMvc.perform(post("/api/buyer/rfqs/" + rfqCode + "/accept")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.data.contactAvailable", is(true)));

        // 6. Buyer gets contact details -> 200 OK + contact info
        mockMvc.perform(get("/api/buyer/rfqs/" + rfqCode + "/contact")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactName", is("KFPCL Manager")))
                .andExpect(jsonPath("$.data.contactPhone", is("9876543210")))
                .andExpect(jsonPath("$.data.contactEmail", is("manager@kfpcl.com")));

        // 7. Check Notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("API Test: Rejection & Re-raising lifecycle")
    void testRejectionAndReRaiseApi() throws Exception {
        // 1. Create RFQ
        BuyerCreateRfqRequest createReq = BuyerCreateRfqRequest.builder()
                .productId(product.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        String createResponse = mockMvc.perform(post("/api/buyer/rfqs")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String rfqCode = objectMapper.readTree(createResponse).path("data").path("rfqCode").asText();

        // 2. Simulate Admin Response via dummy data
        Rfq rfqEntity = rfqRepository.findByRfqCode(rfqCode).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 90000.0, "1000 KG", "7 Days", "Standard rate", "Sales", "9999999999", "sales@kfpcl.com");

        // 3. Buyer Rejects
        BuyerRejectRfqRequest rejectReq = new BuyerRejectRfqRequest("Quoted price is too high");
        mockMvc.perform(post("/api/buyer/rfqs/" + rfqCode + "/reject")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")))
                .andExpect(jsonPath("$.data.rejectionReason", is("Quoted price is too high")));

        // 4. Buyer Re-raises RFQ
        BuyerReRaiseRfqRequest reRaiseReq = BuyerReRaiseRfqRequest.builder()
                .quantity("1200 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Please reconsider discount")
                .build();

        mockMvc.perform(post("/api/buyer/rfqs/" + rfqCode + "/re-raise")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reRaiseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.parentRfqCode", is(rfqCode)))
                .andExpect(jsonPath("$.data.quantity", is("1200 KG")));
    }
}
