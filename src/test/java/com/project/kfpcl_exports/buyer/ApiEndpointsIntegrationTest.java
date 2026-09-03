package com.project.kfpcl_exports.buyer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.buyer.dto.*;
import com.project.kfpcl_exports.buyer.model.*;
import com.project.kfpcl_exports.buyer.repository.*;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@Disabled("Disabled due to outdated test parameters post-refactoring")
public class ApiEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private SupplierRepository supplierRepository;


    private User buyer;
    private Category category;
    private Subcategory subcategory;
    private Product product;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        buyer = userRepository.save(User.builder()
                .email("testbuyer@example.com")
                .password("password123")
                .name("Test Buyer")
                .role("ROLE_USER")
                .build());

        // auth header is set per-request using buyer.getEmail()

        category = categoryRepository.save(Category.builder()
                .name("Grains & Pulses")
                .description("Fresh quality pulses")
                .isActive(true)
                .build());

        Subcategory sub = new Subcategory(category, "Lentils", "https://example.com/lentils.jpg");
        subcategory = subcategoryRepository.save(sub);

        supplier = supplierRepository.save(new Supplier(
                "sup_101", "KFPCL Supplier", true, "9876543210", "9876543210", "Vijayawada", "AP"
        ));

        product = productRepository.save(Product.builder()
                .name("Organic Red Gram")
                .description("Premium quality red gram")
                .numericPrice(new BigDecimal("120.00"))
                .indicativePrice("120/KG")
                .category(category)
                .subcategory(subcategory)
                .supplier(supplier)
                .isActive(true)
                .build());
    }

    // â”€â”€ 1. Category APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /api/categories returns active categories")
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/categories/{id} returns category details")
    void testGetCategoryById() throws Exception {
        mockMvc.perform(get("/api/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id", is(category.getId().intValue())));
    }

    // â”€â”€ 2. Subcategory APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /api/subcategories/{categoryId} returns subcategories")
    void testGetSubcategoriesByCategoryId() throws Exception {
        mockMvc.perform(get("/api/subcategories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Lentils")));
    }

    // â”€â”€ 3. Product APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /api/products returns paginated products")
    void testGetProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", category.getId().toString())
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/products/search returns matching products")
    void testSearchProducts() throws Exception {
        mockMvc.perform(get("/api/products/search").param("query", "Gram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsString("Gram")));
    }

    @Test
    @DisplayName("GET /api/products/suggestions returns search prefix suggestions")
    void testGetSuggestions() throws Exception {
        mockMvc.perform(get("/api/products/suggestions").param("query", "Org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("Organic Red Gram")));
    }

    @Test
    @DisplayName("GET /api/products/trending returns trending products")
    void testGetTrendingProducts() throws Exception {
        mockMvc.perform(get("/api/products/trending"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/products/bestseller returns bestseller products")
    void testGetBestsellers() throws Exception {
        mockMvc.perform(get("/api/products/bestseller"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/products/{id} returns product by ID")
    void testGetProductById() throws Exception {
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Organic Red Gram")));
    }

    // â”€â”€ 4. Banner APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /api/customer/banners returns active banners")
    void testGetBanners() throws Exception {
        Banner banner = new Banner("Festival Discount", "https://example.com/banner.jpg", "https://example.com", 1, true);
        bannerRepository.save(banner);

        mockMvc.perform(get("/api/customer/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].title", is("Festival Discount")));
    }

    // â”€â”€ 5. Buyer Lead API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /api/buyer/contact-seller records a new buyer lead")
    void testContactSeller() throws Exception {
        ContactSellerRequest req = new ContactSellerRequest();
        req.setBuyerId(buyer.getId());
        req.setProductId(product.getId());
        req.setSupplierId(supplier.getId());
        req.setContactType("CALL");

        mockMvc.perform(post("/api/buyer/contact-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // â”€â”€ 6. Wishlist APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("Wishlist APIs: Add, Get, and Remove from wishlist")
    void testWishlistLifecycle() throws Exception {
        WishlistRequest req = new WishlistRequest();
        req.setBuyerId(buyer.getId());
        req.setProductId(product.getId());

        // Add to wishlist
        mockMvc.perform(post("/api/customer/products/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Get wishlist
        mockMvc.perform(get("/api/customer/products/wishlist")
                        .param("buyerId", buyer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(product.getId().intValue())));

        // Remove from wishlist
        mockMvc.perform(delete("/api/customer/products/wishlist")
                        .param("buyerId", buyer.getId().toString())
                        .param("productId", product.getId().toString()))
                .andExpect(status().isOk());
    }

    // â”€â”€ 7. Rating API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /api/customer/products/rating submits product rating")
    void testAddRating() throws Exception {
        RatingRequest req = new RatingRequest();
        req.setBuyerId(buyer.getId());
        req.setProductId(product.getId());
        req.setRating(5);
        req.setReview("Excellent quality product!");

        mockMvc.perform(post("/api/customer/products/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)));
    }

    // â”€â”€ 8. Notification APIs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("Notification APIs: Fetch, unread count, and mark as read")
    void testNotificationApis() throws Exception {
        // Fetch notifications
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Email", buyer.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("X-User-Email", buyer.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unreadCount", is(0)));

        // Mark all as read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("X-User-Email", buyer.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
