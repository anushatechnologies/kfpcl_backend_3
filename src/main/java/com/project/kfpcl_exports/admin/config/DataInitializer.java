package com.project.kfpcl_exports.admin.config;

import com.project.kfpcl_exports.admin.model.*;
import com.project.kfpcl_exports.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// @Component("adminDataInitializer") // Disabled: dummy data removed for production
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;
    private final RfqRepository rfqRepository;
    private final StoreRepository storeRepository;
    private final BannerRepository bannerRepository;
    private final CheckoutSettingsRepository checkoutSettingsRepository;
    private final PolicyRepository policyRepository;

    @Override
    public void run(String... args) {
        // Seed Admin Account
        if (adminUserRepository.findByEmail("admin@kfpclexports.com").isEmpty()) {
            adminUserRepository.save(AdminUser.builder()
                    .email("admin@kfpclexports.com")
                    .password("admin123")
                    .name("KFPCL Super Admin")
                    .role("ADMIN")
                    .build());
        }

        // Seed Categories
        if (categoryRepository.count() == 0) {
            Category cat1 = categoryRepository.save(Category.builder()
                    .name("Fresh Spices")
                    .description("Premium Grade Export Spices from Farmers")
                    .imageUrl("https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=600&q=80")
                    .discount(15.0)
                    .active(true)
                    .build());

            Category cat2 = categoryRepository.save(Category.builder()
                    .name("Organic Grains & Pulses")
                    .description("High quality export grains and lentils")
                    .imageUrl("https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80")
                    .discount(10.0)
                    .active(true)
                    .build());

            // Seed Subcategories
            Subcategory sub1 = subcategoryRepository.save(Subcategory.builder()
                    .name("Red Chili & Turmeric")
                    .description("Ground & Whole High-Curcumin Turmeric")
                    .categoryId(cat1.getId())
                    .categoryName(cat1.getName())
                    .active(true)
                    .build());

            Subcategory sub2 = subcategoryRepository.save(Subcategory.builder()
                    .name("Basmati & Non-Basmati Rice")
                    .description("Export Quality Long Grain Rice")
                    .categoryId(cat2.getId())
                    .categoryName(cat2.getName())
                    .active(true)
                    .build());

            // Seed Products
            Product p1 = productRepository.save(Product.builder()
                    .title("Organic Salem Turmeric Powder")
                    .description("Curcumin content > 4.5%, Grade A Organic Certified")
                    .price(450.0)
                    .originalPrice(520.0)
                    .stock(5000)
                    .unit("kg")
                    .categoryId(cat1.getId())
                    .categoryName(cat1.getName())
                    .subcategoryId(sub1.getId())
                    .subcategoryName(sub1.getName())
                    .mainImageUrl("https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&w=600&q=80")
                    .rating(4.9)
                    .reviewCount(42)
                    .trending(true)
                    .active(true)
                    .build());

            Product p2 = productRepository.save(Product.builder()
                    .title("1121 Steam Basmati Rice")
                    .description("Extra long grain, average length 8.35mm")
                    .price(1250.0)
                    .originalPrice(1400.0)
                    .stock(20000)
                    .unit("metric ton")
                    .categoryId(cat2.getId())
                    .categoryName(cat2.getName())
                    .subcategoryId(sub2.getId())
                    .subcategoryName(sub2.getName())
                    .mainImageUrl("https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80")
                    .rating(4.8)
                    .reviewCount(28)
                    .trending(true)
                    .active(true)
                    .build());

            // Seed RFQ
            Rfq sampleRfq = rfqRepository.save(Rfq.builder()
                    .rfqNumber("RFQ-2026-001")
                    .customerId(101L)
                    .customerName("Global Agri Trading LLC")
                    .customerEmail("buyer@globalagri.com")
                    .customerPhone("+1 555 019 2831")
                    .productName("Organic Salem Turmeric Powder")
                    .quantity(500)
                    .unit("kg")
                    .targetPrice("$4.00 / kg")
                    .shippingTerms("FOB Chennai")
                    .destinationCountry("United States")
                    .details("Requesting lab test certificates and COA with quotation.")
                    .status("PENDING")
                    .build());
        }

        // Seed Store
        if (storeRepository.count() == 0) {
            storeRepository.save(Store.builder()
                    .name("KFPCL Central Export Warehouse")
                    .address("Plot 45, Agro Processing Zone")
                    .city("Trichy")
                    .state("Tamil Nadu")
                    .country("India")
                    .phone("+91 9876543210")
                    .email("warehouse@kfpclexports.com")
                    .active(true)
                    .build());
        }

        // Seed Banner
        if (bannerRepository.count() == 0) {
            bannerRepository.save(Banner.builder()
                    .title("Direct Farmer to Global Buyer Export Deals")
                    .imageUrl("https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&w=1200&q=80")
                    .targetUrl("/products")
                    .position("HERO")
                    .active(true)
                    .build());
        }

        // Seed Checkout Settings
        if (checkoutSettingsRepository.count() == 0) {
            checkoutSettingsRepository.save(CheckoutSettings.builder()
                    .minimumOrderAmount(100.0)
                    .taxPercentage(5.0)
                    .shippingFee(50.0)
                    .enableCod(false)
                    .enableOnlinePayment(true)
                    .currency("USD")
                    .build());
        }

        // Seed Policies
        List<String> types = List.of("terms", "privacy", "refund", "shipping", "export_policy");
        for (String t : types) {
            if (policyRepository.findByType(t).isEmpty()) {
                policyRepository.save(Policy.builder()
                        .type(t)
                        .title(t.substring(0, 1).toUpperCase() + t.substring(1).replace("_", " ") + " Policy")
                        .content("Official " + t + " policy for KFPCL Exports. All international trade complies with APEDA and DGFT standards.")
                        .build());
            }
        }
    }
}
