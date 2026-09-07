package com.project.kfpcl_exports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Banner;
import com.project.kfpcl_exports.admin.model.Customer;
import com.project.kfpcl_exports.admin.model.Store;
import com.project.kfpcl_exports.admin.repository.BannerRepository;
import com.project.kfpcl_exports.admin.repository.CustomerRepository;
import com.project.kfpcl_exports.admin.repository.StoreRepository;
import com.project.kfpcl_exports.buyer.dto.BuyerCreateRfqRequest;
import com.project.kfpcl_exports.buyer.dto.WishlistRequest;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AllRemainingModulesApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository buyerProductRepository;

    // =========================================================================
    // 1. STORE MANAGEMENT APIS (/api/stores)
    // =========================================================================

    @Test
    @DisplayName("CRUD Store APIs: Create, Read, Update, and Delete Store")
    void testStoreCrudLifecycle() throws Exception {
        // 1. Create Store
        Store store = new Store();
        store.setName("Hyderabad Agro Hub");
        store.setCity("Hyderabad");
        store.setState("Telangana");
        store.setCountry("India");
        store.setActive(true);

        MvcResult createResult = mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Hyderabad Agro Hub"))
                .andReturn();

        Store created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Store.class);
        Long storeId = created.getId();

        // 2. Get All Stores
        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 3. Get Store By ID
        mockMvc.perform(get("/api/stores/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hyderabad Agro Hub"));

        // 4. Update Store
        Store updateDetails = new Store();
        updateDetails.setName("Hyderabad Agro Mega Hub");
        mockMvc.perform(put("/api/stores/" + storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hyderabad Agro Mega Hub"));

        // 5. Delete Store
        mockMvc.perform(delete("/api/stores/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 2. BANNERS API (/api/banners)
    // =========================================================================

    @Test
    @DisplayName("Banner APIs: Create and List Banners")
    void testBannerLifecycle() throws Exception {
        Banner banner = new Banner();
        banner.setTitle("Organic Harvest Season 2026");
        banner.setTargetUrl("https://kfpclexports.com/harvest");
        banner.setPosition("TOP_HERO");
        banner.setActive(true);

        mockMvc.perform(post("/api/banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(banner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Organic Harvest Season 2026"));

        mockMvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 3. CUSTOMER MANAGEMENT APIS (/api/admin/customers)
    // =========================================================================

    @Test
    @DisplayName("Customer APIs: List and Update Customer Status")
    void testCustomerManagement() throws Exception {
        Customer customer = new Customer();
        customer.setName("Global Import Traders");
        customer.setEmail("buyer@globaltraders.com");
        customer.setPhone("9988776655");
        customer.setCompanyName("Global Traders Inc");
        customer.setStatus("ACTIVE");
        Customer saved = customerRepository.save(customer);

        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/admin/customers/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Global Import Traders"));

        mockMvc.perform(patch("/api/admin/customers/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // =========================================================================
    // 4. ADMIN DASHBOARD APIS (/api/admin/dashboard)
    // =========================================================================

    @Test
    @DisplayName("Admin Dashboard: Summary, Analytics, Product Performance")
    void testDashboardEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalRevenue").exists());

        mockMvc.perform(get("/api/admin/dashboard/analytics?period=month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("month"))
                .andExpect(jsonPath("$.salesTrend").isArray());

        mockMvc.perform(get("/api/admin/dashboard/product-performance"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 5. BUYER PRODUCT & CATEGORY BROWSING APIS (/api/buyer/**)
    // =========================================================================

    @Test
    @DisplayName("Buyer APIs: Products, Search, Suggestions, Categories")
    void testBuyerCatalogBrowsing() throws Exception {
        Product p = new Product();
        p.setName("Indian Alphonso Mango Export Grade");
        p.setNumericPrice(new BigDecimal("1200.00"));
        p.setIndicativePrice("1200.00");
        p.setIsActive(true);
        buyerProductRepository.save(p);

        mockMvc.perform(get("/api/buyer/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/buyer/products/suggestions?query=Alphonso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/buyer/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/buyer/products/bestseller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/buyer/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // 6. BUYER RFQ (REQUEST FOR QUOTATION) APIS (/api/buyer/rfqs)
    // =========================================================================

    @Test
    @DisplayName("Buyer RFQ API: Create and Retrieve RFQ")
    void testBuyerRfqWorkflow() throws Exception {
        Product p = new Product();
        p.setName("Export Quality Spices - Cardamom");
        p.setNumericPrice(new BigDecimal("2500.00"));
        p.setIsActive(true);
        Product savedProd = buyerProductRepository.save(p);

        BuyerCreateRfqRequest rfqReq = BuyerCreateRfqRequest.builder()
                .productId(savedProd.getId())
                .quantity("500 kg")
                .deliveryLocation("Dubai Port, UAE")
                .buyerMessage("Looking for urgent air freight delivery")
                .phoneNumber("9110009988")
                .build();

        mockMvc.perform(post("/api/buyer/rfqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rfqReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryLocation").value("Dubai Port, UAE"));

        mockMvc.perform(get("/api/buyer/rfqs?phoneNumber=9110009988"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 7. BUYER WISHLIST APIS (/api/customer/products/wishlist)
    // =========================================================================

    @Test
    @DisplayName("Wishlist APIs: Add, List, and Delete Wishlist items")
    void testWishlistWorkflow() throws Exception {
        Product p = new Product();
        p.setName("Pure Turmeric Fingers 500g");
        p.setNumericPrice(new BigDecimal("180.00"));
        p.setIsActive(true);
        Product savedProd = buyerProductRepository.save(p);

        WishlistRequest req = new WishlistRequest();
        req.setBuyerId("buyer_user_100");
        req.setProductId(savedProd.getId());

        mockMvc.perform(post("/api/customer/products/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/products/wishlist?buyerId=buyer_user_100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(delete("/api/customer/products/wishlist?buyerId=buyer_user_100&productId=" + savedProd.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("Product removed from wishlist"));
    }

    // =========================================================================
    // 8. ALIAS CATALOG ROUTES (/api/v1/admin/catalog/products)
    // =========================================================================

    @Test
    @DisplayName("Verify /api/v1/admin/catalog/products and catalog aliases return 200 OK")
    void testAdminCatalogAliasRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/admin/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/admin/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
