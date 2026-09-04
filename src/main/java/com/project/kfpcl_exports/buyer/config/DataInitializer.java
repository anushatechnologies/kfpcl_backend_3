package com.project.kfpcl_exports.buyer.config;

import com.project.kfpcl_exports.buyer.enums.NotificationType;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import com.project.kfpcl_exports.buyer.model.*;
import com.project.kfpcl_exports.buyer.repository.*;
import com.project.kfpcl_exports.buyer.service.NotificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// @Component("buyerDataInitializer") // Disabled: dummy data removed for production
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final RfqRepository rfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            RfqRepository rfqRepository,
            RfqResponseRepository rfqResponseRepository,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder,
            com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.rfqRepository = rfqRepository;
        this.rfqResponseRepository = rfqResponseRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.adminProductRepository = adminProductRepository;
    }

    private final com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository;

    @Override
    public void run(String... args) {
        // 1. Seed Buyer User
        User buyer = userRepository.findByEmail("buyer1@kfpcl.com").orElseGet(() -> {
            User b = User.builder()
                    .email("buyer1@kfpcl.com")
                    .name("Buyer One")
                    .phoneNumber("9876543210")
                    .password(passwordEncoder.encode("buyer123"))
                    .role("ROLE_USER")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(b);
        });

        // 1b. Seed Default Supplier
        Supplier supplier = supplierRepository.findById("sup_101").orElseGet(() -> {
            Supplier s = new Supplier("sup_101", "KFPCL Supplier", true, "9876543210", "9876543210", "Vijayawada", "AP");
            return supplierRepository.save(s);
        });

        // 2. Seed Category and Sample Product
        Category category = categoryRepository.findByIsActiveTrue().stream().findFirst().orElseGet(() -> {
            Category cat = Category.builder()
                    .name("Grains & Cereals")
                    .description("High quality agricultural grains and cereals")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return categoryRepository.save(cat);
        });

        com.project.kfpcl_exports.admin.model.Product adminProduct = adminProductRepository.findById(1L).orElseGet(() -> {
            com.project.kfpcl_exports.admin.model.Product ap = com.project.kfpcl_exports.admin.model.Product.builder()
                    .id(1L)
                    .title("Basmati Rice Premium")
                    .description("Premium aged long grain aromatic Basmati rice")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return adminProductRepository.save(ap);
        });

        // 3. Seed Dummy Responded RFQ 1 (RFQ-2026-000001) for Acceptance Flow
        if (rfqRepository.findByRfqCode("RFQ-2026-000001").isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            Rfq sampleRfq1 = Rfq.builder()
                    .rfqCode("RFQ-2026-000001")
                    .buyer(buyer)
                    .product(adminProduct)
                    .quantity("1000 KG")
                    .deliveryLocation("Vijayawada")
                    .buyerMessage("Need best quotation including transport")
                    .status(RfqStatus.RESPONDED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Rfq savedRfq1 = rfqRepository.save(sampleRfq1);

            RfqResponse dummyAdminResponse1 = RfqResponse.builder()
                    .rfq(savedRfq1)
                    .quotedPrice(62000.0)
                    .availableQuantity("1000 KG")
                    .deliveryTime("4 Days")
                    .responseMessage("Price includes transportation to Vijayawada")
                    .contactName("KFPCL Sales Team")
                    .contactPhone("9876543210")
                    .contactEmail("sales@kfpcl.com")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            rfqResponseRepository.save(dummyAdminResponse1);
            savedRfq1.getResponses().add(dummyAdminResponse1);
            rfqRepository.save(savedRfq1);

            notificationService.createNotification(
                    buyer,
                    NotificationType.RFQ_RESPONSE_RECEIVED,
                    "RFQ Response Received",
                    "A response has been received for RFQ RFQ-2026-000001.",
                    "RFQ",
                    "RFQ-2026-000001"
            );
        }

        // 4. Seed Dummy Responded RFQ 2 (RFQ-2026-000002) for Rejection & Re-raise Flow
        if (rfqRepository.findByRfqCode("RFQ-2026-000002").isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            Rfq sampleRfq2 = Rfq.builder()
                    .rfqCode("RFQ-2026-000002")
                    .buyer(buyer)
                    .product(adminProduct)
                    .quantity("2000 KG")
                    .deliveryLocation("Guntur")
                    .buyerMessage("Looking for volume discount for 2000 KG")
                    .status(RfqStatus.RESPONDED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Rfq savedRfq2 = rfqRepository.save(sampleRfq2);

            RfqResponse dummyAdminResponse2 = RfqResponse.builder()
                    .rfq(savedRfq2)
                    .quotedPrice(140000.0)
                    .availableQuantity("2000 KG")
                    .deliveryTime("6 Days")
                    .responseMessage("Standard bulk pricing applies (Dummy Admin Quotation)")
                    .contactName("KFPCL Sales Executive")
                    .contactPhone("9876543210")
                    .contactEmail("sales@kfpcl.com")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            rfqResponseRepository.save(dummyAdminResponse2);
            savedRfq2.getResponses().add(dummyAdminResponse2);
            rfqRepository.save(savedRfq2);

            notificationService.createNotification(
                    buyer,
                    NotificationType.RFQ_RESPONSE_RECEIVED,
                    "RFQ Response Received",
                    "A response has been received for RFQ RFQ-2026-000002.",
                    "RFQ",
                    "RFQ-2026-000002"
            );
        }
    }
}
