package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.*;
import com.project.kfpcl_exports.buyer.enums.NotificationType;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import com.project.kfpcl_exports.buyer.exception.RfqException;
import com.project.kfpcl_exports.buyer.model.*;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import com.project.kfpcl_exports.buyer.repository.RfqRepository;
import com.project.kfpcl_exports.buyer.repository.RfqResponseRepository;
import com.project.kfpcl_exports.buyer.util.RfqCodeGenerator;
import com.project.kfpcl_exports.admin.model.DeviceToken;
import com.project.kfpcl_exports.admin.repository.DeviceTokenRepository;
import com.project.kfpcl_exports.service.FcmTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class RfqService {

    private final RfqRepository rfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final ProductRepository buyerProductRepository;
    private final com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository;
    private final com.project.kfpcl_exports.admin.repository.RfqRepository adminRfqRepository;
    private final com.project.kfpcl_exports.admin.repository.QuotationRepository adminQuotationRepository;
    private final RfqCodeGenerator rfqCodeGenerator;
    private final NotificationService notificationService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmTokenService fcmTokenService;

    public RfqService(
            RfqRepository rfqRepository,
            RfqResponseRepository rfqResponseRepository,
            ProductRepository buyerProductRepository,
            com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository,
            com.project.kfpcl_exports.admin.repository.RfqRepository adminRfqRepository,
            com.project.kfpcl_exports.admin.repository.QuotationRepository adminQuotationRepository,
            RfqCodeGenerator rfqCodeGenerator,
            NotificationService notificationService,
            @Autowired(required = false) DeviceTokenRepository deviceTokenRepository,
            @Autowired(required = false) FcmTokenService fcmTokenService
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqResponseRepository = rfqResponseRepository;
        this.buyerProductRepository = buyerProductRepository;
        this.adminProductRepository = adminProductRepository;
        this.adminRfqRepository = adminRfqRepository;
        this.adminQuotationRepository = adminQuotationRepository;
        this.rfqCodeGenerator = rfqCodeGenerator;
        this.notificationService = notificationService;
        this.deviceTokenRepository = deviceTokenRepository;
        this.fcmTokenService = fcmTokenService;
    }

    /**
     * 1. Create a new RFQ for the authenticated buyer.
     */
    public BuyerRfqResponseDto createRfq(User buyer, BuyerCreateRfqRequest request) {
        com.project.kfpcl_exports.buyer.model.Product buyerProduct = null;
        // The catalog product is the authoritative source for the store selected by the buyer.
        com.project.kfpcl_exports.admin.model.Product selectedAdminProduct = request.getProductId() == null
                ? null
                : adminProductRepository.findById(request.getProductId()).orElse(null);

        String explicitOrExtractedName = extractProductName(request);

        // 1. If explicit product ID was provided, look it up
        if (request.getProductId() != null) {
            buyerProduct = buyerProductRepository.findById(request.getProductId()).orElse(null);
            if (buyerProduct == null) {
                com.project.kfpcl_exports.admin.model.Product adminProd = selectedAdminProduct;
                if (adminProd != null) {
                    com.project.kfpcl_exports.buyer.model.Product synced = com.project.kfpcl_exports.buyer.model.Product.builder()
                            .id(adminProd.getId())
                            .name(adminProd.getTitle() != null ? adminProd.getTitle() : "Commodity")
                            .description(adminProd.getDescription())
                            .mainImageUrl(adminProd.getMainImageUrl())
                            .imageUrl(adminProd.getMainImageUrl())
                            .storeId(adminProd.getStoreId())
                            .storeName(adminProd.getStoreName())
                            .isActive(Boolean.TRUE.equals(adminProd.getActive()))
                            .createdAt(LocalDateTime.now())
                            .build();
                    try {
                        buyerProduct = buyerProductRepository.save(synced);
                    } catch (Exception e) {
                        synced.setId(null);
                        buyerProduct = buyerProductRepository.save(synced);
                    }
                }
            }
        }

        // 2. If a specific product name was passed or extracted from quantity/subject, match or create it
        if (explicitOrExtractedName != null && !explicitOrExtractedName.isBlank()) {
            boolean mismatch = (buyerProduct != null && request.getProductName() != null && !buyerProduct.getName().equalsIgnoreCase(request.getProductName()));
            if (buyerProduct == null || mismatch) {
                // Search by name in buyer_products
                List<com.project.kfpcl_exports.buyer.model.Product> matches = buyerProductRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrue(explicitOrExtractedName);
                if (!matches.isEmpty()) {
                    buyerProduct = matches.get(0);
                } else {
                    // Search in admin products
                    List<com.project.kfpcl_exports.admin.model.Product> adminMatches = adminProductRepository.findByTitleContainingIgnoreCase(explicitOrExtractedName);
                    if (!adminMatches.isEmpty()) {
                        com.project.kfpcl_exports.admin.model.Product ap = adminMatches.get(0);
                        com.project.kfpcl_exports.buyer.model.Product synced = com.project.kfpcl_exports.buyer.model.Product.builder()
                                .id(ap.getId())
                                .name(ap.getTitle())
                                .description(ap.getDescription())
                                .mainImageUrl(ap.getMainImageUrl())
                                .imageUrl(ap.getMainImageUrl())
                                .storeId(ap.getStoreId())
                                .storeName(ap.getStoreName())
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        try {
                            buyerProduct = buyerProductRepository.save(synced);
                        } catch (Exception ignored) {
                            synced.setId(null);
                            buyerProduct = buyerProductRepository.save(synced);
                        }
                    } else {
                        // Dynamically create commodity product entry so the RFQ title exactly matches the buyer's requirement
                        com.project.kfpcl_exports.buyer.model.Product custom = com.project.kfpcl_exports.buyer.model.Product.builder()
                                .name(explicitOrExtractedName)
                                .description("Inquired commodity: " + explicitOrExtractedName)
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        buyerProduct = buyerProductRepository.save(custom);
                    }
                }
            }
        }

        // 3. Fallback: Pick any active product in buyer_products table
        if (buyerProduct == null) {
            buyerProduct = buyerProductRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .findFirst()
                    .orElseGet(() -> buyerProductRepository.findAll().stream().findFirst().orElse(null));
        }

        // 4. Fallback: Auto-create a default product entry in buyer_products table
        if (buyerProduct == null) {
            LocalDateTime now = LocalDateTime.now();
            com.project.kfpcl_exports.buyer.model.Product fallback = com.project.kfpcl_exports.buyer.model.Product.builder()
                    .name(explicitOrExtractedName != null ? explicitOrExtractedName : "General Commodity Product")
                    .description("Default product created for RFQ requests")
                    .isActive(true)
                    .createdAt(now)
                    .build();
            buyerProduct = buyerProductRepository.save(fallback);
        }

        StoreAssignment storeAssignment = resolveProductStore(buyerProduct, selectedAdminProduct);
        applyStoreToBuyerProduct(buyerProduct, storeAssignment);

        String rfqCode = rfqCodeGenerator.generateRfqCode();
        LocalDateTime now = LocalDateTime.now();

        // Clean buyer_message: extract embedded name/phone/subject and keep only the actual message
        String cleanMessage = sanitizeAndExtractMessage(request, request.getBuyerMessage());

        // Clean quantity: extract only numeric quantity + unit (e.g. "500 kg"), strip product names
        String cleanQuantity = cleanQuantityField(request);

        String buyerName = request.getBuyerName() != null ? request.getBuyerName() : (buyer != null ? buyer.getName() : null);
        String buyerPhone = request.getBuyerPhone() != null ? request.getBuyerPhone() : (buyer != null ? buyer.getPhoneNumber() : null);

        Rfq rfq = Rfq.builder()
                .rfqCode(rfqCode)
                .buyer(buyer)
                .product(buyerProduct)
                .storeId(storeAssignment.storeId())
                .storeName(storeAssignment.storeName())
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .subject(request.getSubject())
                .fileUrl(request.getFileUrl())
                .quantity(cleanQuantity)
                .deliveryLocation(request.getDeliveryLocation() != null ? request.getDeliveryLocation() : "Default Location")
                .buyerMessage(cleanMessage)
                .status(RfqStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Rfq saved = rfqRepository.save(rfq);
        syncToAdminRfqTable(saved, "PENDING");
        notifyAdminNewRfq(saved);
        return mapToBuyerDto(saved, false);
    }

    private void notifyAdminNewRfq(Rfq rfq) {
        if (deviceTokenRepository == null || fcmTokenService == null) {
            return;
        }
        try {
            List<DeviceToken> adminTokens = deviceTokenRepository.findByUserTypeIgnoreCase("ADMIN");
            String title = "New RFQ Received";
            String body = "Buyer " + (rfq.getBuyerName() != null ? rfq.getBuyerName() : "Customer") +
                    " submitted RFQ #" + rfq.getRfqCode() + " for " +
                    (rfq.getProduct() != null ? rfq.getProduct().getName() : "product");
            for (DeviceToken dt : adminTokens) {
                try {
                    fcmTokenService.sendPushNotification(dt.getToken(), title, body);
                } catch (Exception e) {
                    log.warn("Failed to send FCM push to Admin device token {}: {}", dt.getToken(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to notify Admin of new RFQ: {}", e.getMessage());
        }
    }

    /**
     * 2. Get all RFQs belonging strictly to the authenticated buyer.
     */
    @Transactional(readOnly = true)
    public Page<BuyerRfqResponseDto> getBuyerRfqs(User buyer, RfqStatus status, Pageable pageable) {
        Page<Rfq> page;
        try {
            if (status != null) {
                page = rfqRepository.findByBuyerAndStatusOrderByCreatedAtDesc(buyer, status, pageable);
            } else {
                page = rfqRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable);
            }
        } catch (Exception ex) {
            log.warn("Standard buyer RFQ query encountered error ({}), falling back to native query", ex.getMessage());
            String buyerId = buyer != null ? buyer.getId() : "";
            String phone = buyer != null ? buyer.getPhoneNumber() : "";
            String statusStr = status != null ? status.name() : null;
            page = rfqRepository.findRfqsNative(buyerId, phone, statusStr, pageable);
        }

        return page.map(rfq -> mapToBuyerDto(rfq, rfq.getStatus() == RfqStatus.ACCEPTED));
    }

    public BuyerRfqResponseDto getBuyerRfqDetail(User buyer, String rfqIdOrCode) {
        Rfq rfq = findRfqAndValidateOwnership(rfqIdOrCode, buyer);
        return mapToBuyerDto(rfq, rfq.getStatus() == RfqStatus.ACCEPTED);
    }

    /**
     * 4. Accept Admin response.
     * Transition: RESPONDED -> ACCEPTED.
     */
    public BuyerRfqResponseDto acceptRfqResponse(User buyer, String rfqIdOrCode) {
        Rfq rfq = findRfqAndValidateOwnership(rfqIdOrCode, buyer);

        if (rfq.getStatus() == RfqStatus.ACCEPTED) {
            throw RfqException.alreadyAccepted("RFQ is already accepted");
        }
        if (rfq.getStatus() == RfqStatus.REJECTED) {
            throw RfqException.invalidStatus("Cannot accept a rejected RFQ");
        }
        if (rfq.getStatus() != RfqStatus.RESPONDED) {
            throw RfqException.notResponded("RFQ cannot be accepted because it has not been responded to by Admin yet");
        }
        if (rfq.getResponses().isEmpty()) {
            throw RfqException.notResponded("No quotation response found to accept");
        }

        rfq.setStatus(RfqStatus.ACCEPTED);
        rfq.setUpdatedAt(LocalDateTime.now());
        Rfq saved = rfqRepository.save(rfq);
        syncToAdminRfqTable(saved, "ACCEPTED");

        // Transactional notification
        try {
            notificationService.createNotification(
                    buyer,
                    NotificationType.RFQ_ACCEPTED,
                    "RFQ Accepted",
                    "You have successfully accepted the quotation for RFQ " + saved.getRfqCode() + ". Contact details are now accessible.",
                    "RFQ",
                    saved.getRfqCode()
            );
        } catch (Exception e) {
            log.warn("Failed to create RFQ_ACCEPTED notification for RFQ {}: {}", saved.getRfqCode(), e.getMessage());
        }

        return mapToBuyerDto(saved, true);
    }

    /**
     * 5. Reject Admin response.
     * Transition: RESPONDED -> REJECTED.
     */
    public BuyerRfqResponseDto rejectRfqResponse(User buyer, String rfqIdOrCode, String reason) {
        Rfq rfq = findRfqAndValidateOwnership(rfqIdOrCode, buyer);

        if (rfq.getStatus() == RfqStatus.ACCEPTED) {
            throw RfqException.invalidStatus("Cannot reject an already accepted RFQ");
        }
        if (rfq.getStatus() == RfqStatus.REJECTED) {
            throw RfqException.alreadyRejected("RFQ is already rejected");
        }
        if (rfq.getStatus() != RfqStatus.RESPONDED) {
            throw RfqException.notResponded("Cannot reject RFQ before Admin response");
        }

        rfq.setStatus(RfqStatus.REJECTED);
        rfq.setRejectionReason(reason);
        rfq.setUpdatedAt(LocalDateTime.now());
        Rfq saved = rfqRepository.save(rfq);
        syncToAdminRfqTable(saved, "REJECTED");

        // Transactional notification
        try {
            notificationService.createNotification(
                    buyer,
                    NotificationType.RFQ_REJECTED,
                    "RFQ Rejected",
                    "You have rejected the quotation for RFQ " + saved.getRfqCode() + ".",
                    "RFQ",
                    saved.getRfqCode()
            );
        } catch (Exception e) {
            log.warn("Failed to create RFQ_REJECTED notification for RFQ {}: {}", saved.getRfqCode(), e.getMessage());
        }

        return mapToBuyerDto(saved, false);
    }

    /**
     * 6. Re-raise a rejected RFQ.
     * Preserves negotiation history by creating a new RFQ with parentRfqId.
     */
    public BuyerRfqResponseDto reRaiseRfq(User buyer, String rfqIdOrCode, BuyerReRaiseRfqRequest request) {
        Rfq originalRfq = findRfqAndValidateOwnership(rfqIdOrCode, buyer);

        if (originalRfq.getStatus() != RfqStatus.REJECTED) {
            throw RfqException.invalidStatus("Only REJECTED RFQs can be re-raised");
        }

        String newRfqCode = rfqCodeGenerator.generateRfqCode();
        LocalDateTime now = LocalDateTime.now();

        Rfq newRfq = Rfq.builder()
                .rfqCode(newRfqCode)
                .buyer(originalRfq.getBuyer())
                .product(originalRfq.getProduct())
                .storeId(originalRfq.getStoreId())
                .storeName(originalRfq.getStoreName())
                .quantity(request.getQuantity())
                .deliveryLocation(request.getDeliveryLocation())
                .buyerMessage(request.getBuyerMessage())
                .parentRfq(originalRfq)
                .status(RfqStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Rfq saved = rfqRepository.save(newRfq);
        syncToAdminRfqTable(saved, "PENDING");
        return mapToBuyerDto(saved, false);
    }

    /**
     * 7. Get Contact Details.
     * CRITICAL SECURITY RULE: Allowed ONLY when RFQ status == ACCEPTED.
     */
    @Transactional(readOnly = true)
    public ContactResponseDto getContactDetails(User buyer, String rfqIdOrCode) {
        Rfq rfq = findRfqAndValidateOwnership(rfqIdOrCode, buyer);

        if (rfq.getStatus() != RfqStatus.ACCEPTED) {
            throw RfqException.contactNotAvailable("Contact details are available only after accepting the RFQ response");
        }

        RfqResponse latestResponse = rfq.getLatestResponse();
        if (latestResponse == null) {
            throw RfqException.notFound("No quotation response record found for this RFQ");
        }

        return ContactResponseDto.builder()
                .supplierName("KFPCL Farmer Producer Co.")
                .contactPerson(latestResponse.getContactName() != null ? latestResponse.getContactName() : "Anand V.")
                .contactName(latestResponse.getContactName() != null ? latestResponse.getContactName() : "Anand V.")
                .phone(latestResponse.getContactPhone() != null ? latestResponse.getContactPhone() : "+91 94400 12345")
                .contactPhone(latestResponse.getContactPhone() != null ? latestResponse.getContactPhone() : "+91 94400 12345")
                .email(latestResponse.getContactEmail() != null ? latestResponse.getContactEmail() : "sales@kfpcl.org")
                .contactEmail(latestResponse.getContactEmail() != null ? latestResponse.getContactEmail() : "sales@kfpcl.org")
                .dispatchWarehouse("Khammam Hub 02")
                .build();
    }

    // =========================================================================
    // DUMMY ADMIN RESPONSE SEEDER / HELPER (FOR SIMULATION & TESTING)
    // =========================================================================

    public void addDummyAdminResponse(
            Rfq rfq,
            Double quotedPrice,
            String availableQuantity,
            String deliveryTime,
            String responseMessage,
            String contactName,
            String contactPhone,
            String contactEmail
    ) {
        LocalDateTime now = LocalDateTime.now();
        RfqResponse response = RfqResponse.builder()
                .rfq(rfq)
                .quotedPrice(quotedPrice)
                .availableQuantity(availableQuantity)
                .deliveryTime(deliveryTime)
                .responseMessage(responseMessage)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .contactEmail(contactEmail)
                .createdAt(now)
                .updatedAt(now)
                .build();

        rfqResponseRepository.save(response);
        rfq.setStatus(RfqStatus.RESPONDED);
        rfq.setUpdatedAt(now);
        rfq.getResponses().add(0, response);
        rfqRepository.save(rfq);
        syncToAdminRfqTable(rfq, "QUOTED");

        try {
            notificationService.createNotification(
                    rfq.getBuyer(),
                    NotificationType.RFQ_RESPONSE_RECEIVED,
                    "RFQ Response Received",
                    "A response has been received for RFQ " + rfq.getRfqCode() + ".",
                    "RFQ",
                    rfq.getRfqCode()
            );
        } catch (Exception e) {
            log.warn("Failed to create RFQ_RESPONSE_RECEIVED notification for RFQ {}: {}", rfq.getRfqCode(), e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Rfq findRfqAndValidateOwnership(String rfqIdOrCode, User buyer) {
        Rfq rfq = findRfqByIdOrCode(rfqIdOrCode);
        if (buyer != null) {
            boolean matchesId = rfq.getBuyer() != null && rfq.getBuyer().getId() != null && rfq.getBuyer().getId().equals(buyer.getId());
            boolean matchesPhone = rfq.getBuyerPhone() != null && buyer.getPhoneNumber() != null && rfq.getBuyerPhone().equals(buyer.getPhoneNumber());
            if (!matchesId && !matchesPhone) {
                throw RfqException.accessDenied("Access denied: RFQ does not belong to you");
            }
        }
        return rfq;
    }

    public Rfq findRfqByIdOrCode(String rfqIdOrCode) {
        if (rfqIdOrCode == null || rfqIdOrCode.isBlank()) {
            throw RfqException.notFound("RFQ identifier cannot be empty");
        }

        try {
            Long id = Long.parseLong(rfqIdOrCode.trim());
            return rfqRepository.findById(id)
                    .or(() -> rfqRepository.findByRfqCode(rfqIdOrCode.trim()))
                    .orElseThrow(() -> RfqException.notFound("RFQ not found with identifier: " + rfqIdOrCode));
        } catch (NumberFormatException e) {
            return rfqRepository.findByRfqCode(rfqIdOrCode.trim())
                    .orElseThrow(() -> RfqException.notFound("RFQ not found with code: " + rfqIdOrCode));
        }
    }

    private BuyerRfqResponseDto mapToBuyerDto(Rfq rfq, boolean contactAvailable) {
        BuyerRfqResponseDto.ProductSummaryDto productDto = null;
        String prodTitle = null;
        Long prodId = null;
        try {
            if (rfq.getProduct() != null) {
                prodTitle = rfq.getProduct().getName() != null ? rfq.getProduct().getName() : rfq.getProduct().getTitle();
                prodId = rfq.getProduct().getId();
                productDto = BuyerRfqResponseDto.ProductSummaryDto.builder()
                        .id(prodId)
                        .name(prodTitle)
                        .description(rfq.getProduct().getDescription())
                        .imageUrl(rfq.getProduct().getImageUrl())
                        .build();
            }
        } catch (Exception ignored) {}

        String unit = null;
        if (rfq.getQuantity() != null && rfq.getQuantity().contains(" ")) {
            String[] parts = rfq.getQuantity().split("\\s+", 2);
            if (parts.length > 1) {
                unit = parts[1];
            }
        }

        BuyerRfqResponseDto.RfqResponseSummaryDto responseDto = null;
        RfqResponse latest = rfq.getLatestResponse();
        if (latest != null) {
            double total = (latest.getQuotedPrice() != null ? latest.getQuotedPrice() : 0.0);
            try {
                String numPart = rfq.getQuantity().replaceAll("[^0-9.]", "");
                if (!numPart.isEmpty()) {
                    total *= Double.parseDouble(numPart);
                }
            } catch (Exception ignored) {}

            responseDto = BuyerRfqResponseDto.RfqResponseSummaryDto.builder()
                    .quoteId("QUO-" + latest.getId())
                    .quotedPrice(latest.getQuotedPrice())
                    .offeredPrice(latest.getQuotedPrice())
                    .totalAmount(total)
                    .availableQuantity(latest.getAvailableQuantity())
                    .deliveryTime(latest.getDeliveryTime())
                    .leadTime(latest.getDeliveryTime())
                    .notes(latest.getResponseMessage())
                    .responseMessage(latest.getResponseMessage())
                    .status("PENDING_BUYER_ACTION")
                    .createdAt(latest.getCreatedAt())
                    .build();
        }

        String bName = rfq.getBuyerName();
        String bPhone = rfq.getBuyerPhone();
        try {
            if (rfq.getBuyer() != null) {
                if (bName == null || bName.isBlank()) bName = rfq.getBuyer().getName();
                if (bPhone == null || bPhone.isBlank()) bPhone = rfq.getBuyer().getPhoneNumber();
            }
        } catch (Exception ignored) {}

        return BuyerRfqResponseDto.builder()
                .id(rfq.getId())
                .rfqId(rfq.getId().toString())
                .rfqCode(rfq.getRfqCode())
                .product(productDto)
                .productId(prodId)
                .title(prodTitle)
                .productName(prodTitle)
                .buyerName(bName)
                .buyerPhone(bPhone)
                .quantity(rfq.getQuantity())
                .unit(unit)
                .deliveryLocation(rfq.getDeliveryLocation())
                .subject(rfq.getSubject())
                .buyerMessage(rfq.getBuyerMessage())
                .fileUrl(rfq.getFileUrl())
                .status(rfq.getStatus())
                .parentRfqId(rfq.getParentRfq() != null ? rfq.getParentRfq().getId() : null)
                .parentRfqCode(rfq.getParentRfq() != null ? rfq.getParentRfq().getRfqCode() : null)
                .rejectionReason(rfq.getRejectionReason())
                .createdAt(rfq.getCreatedAt())
                .updatedAt(rfq.getUpdatedAt())
                .response(responseDto)
                .contactAvailable(contactAvailable)
                .build();
    }

    private String extractProductName(BuyerCreateRfqRequest request) {
        if (request == null) return null;

        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            return request.getProductName().trim();
        }

        // Check if subject specifies a commodity (avoid generic subjects like "price Enquiry")
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            String s = request.getSubject().trim();
            String lower = s.toLowerCase();
            if (!lower.contains("price enquiry") && !lower.contains("enquiry") && !lower.contains("rfq") && !lower.equals("inquiry")) {
                return s;
            }
        }

        // Check if quantity has commodity name, e.g. "30 Herbal Hair Oil (Standard pack)"
        if (request.getQuantity() != null && !request.getQuantity().isBlank()) {
            String q = request.getQuantity().trim();
            // Remove leading numbers and operators: "30 Herbal Hair Oil (Standard pack)" -> "Herbal Hair Oil (Standard pack)"
            String stripped = q.replaceFirst("^[0-9]+[\\s\\-xX*]*", "").trim();
            // Remove packaging suffixes like "(Standard pack)", "Standard pack", "(pack)"
            String cleaned = stripped.replaceAll("(?i)\\s*\\(standard pack\\)", "")
                                     .replaceAll("(?i)\\s*standard pack", "")
                                     .replaceAll("(?i)\\s*\\(pack\\)", "")
                                     .trim();
            if (cleaned.length() >= 3 && !cleaned.matches("(?i)^(kg|g|mt|ton|tons|metric tons|pieces|pcs|units|litres|l|box|boxes|cartons)$")) {
                return cleaned;
            }
        }

        return null;
    }

    /**
     * Cleans the quantity field to store ONLY the numeric quantity + unit.
     * Example: "10 Aashirvaad Shudh Chakki Atta (Standard pack)" → "10"
     * Example: "500 kg" → "500 kg"
     * Example: "5,000 KG" → "5,000 KG"
     * Example: "500 Standard pack" → "500"
     * If the product name is embedded in quantity, it is extracted and set on the request.
     */
    private String cleanQuantityField(BuyerCreateRfqRequest request) {
        if (request == null || request.getQuantity() == null || request.getQuantity().isBlank()) {
            return "1";
        }

        String raw = request.getQuantity().trim();

        // Match leading number (with commas/decimals) and optional unit
        java.util.regex.Matcher qtyMatcher = java.util.regex.Pattern
                .compile("^([0-9][0-9,.]*)\\s*(kg|g|mt|ton|tons|metric tons|pieces|pcs|units|litres|liters|l|box|boxes|cartons|KG|MT|L)?(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(raw);

        if (qtyMatcher.find()) {
            String numericPart = qtyMatcher.group(1).trim();
            String unitPart = qtyMatcher.group(2) != null ? qtyMatcher.group(2).trim() : "";
            String remainder = qtyMatcher.group(3) != null ? qtyMatcher.group(3).trim() : "";

            // If there's remaining text after number+unit, it's likely a product name
            if (!remainder.isBlank()) {
                // Remove packaging suffixes
                String productCandidate = remainder
                        .replaceAll("(?i)\\s*\\(standard pack\\)", "")
                        .replaceAll("(?i)\\s*standard pack", "")
                        .replaceAll("(?i)\\s*\\(pack\\)", "")
                        .trim();

                // Set as productName if not already set
                if (productCandidate.length() >= 3
                        && (request.getProductName() == null || request.getProductName().isBlank())) {
                    request.setProductName(productCandidate);
                }
            }

            // Return clean quantity: "500 kg", "10", "5,000 KG"
            return unitPart.isBlank() ? numericPart : (numericPart + " " + unitPart);
        }

        // Fallback: return as-is
        return raw;
    }

    private record StoreAssignment(Long storeId, String storeName) {}

    /**
     * Resolves the store from the catalog product whenever possible.  Buyer-product
     * rows are a display mirror and older rows may not contain store information.
     */
    private StoreAssignment resolveProductStore(
            com.project.kfpcl_exports.buyer.model.Product buyerProduct,
            com.project.kfpcl_exports.admin.model.Product preferredAdminProduct) {

        Long storeId = buyerProduct != null ? buyerProduct.getStoreId() : null;
        String storeName = buyerProduct != null ? buyerProduct.getStoreName() : null;

        com.project.kfpcl_exports.admin.model.Product sourceProduct = preferredAdminProduct;
        if (sourceProduct == null) {
            sourceProduct = findMatchingAdminProduct(buyerProduct);
        }

        if (sourceProduct != null) {
            if (sourceProduct.getStoreId() != null) {
                storeId = sourceProduct.getStoreId();
            }
            if (hasText(sourceProduct.getStoreName())) {
                storeName = sourceProduct.getStoreName().trim();
            }
        }

        return new StoreAssignment(storeId, storeName);
    }

    private com.project.kfpcl_exports.admin.model.Product findMatchingAdminProduct(
            com.project.kfpcl_exports.buyer.model.Product buyerProduct) {
        if (buyerProduct == null) {
            return null;
        }

        String buyerProductName = buyerProduct.getName();
        if (buyerProduct.getId() != null) {
            com.project.kfpcl_exports.admin.model.Product byId = adminProductRepository
                    .findById(buyerProduct.getId())
                    .orElse(null);
            if (byId != null && (!hasText(buyerProductName) || namesMatch(byId.getTitle(), buyerProductName))) {
                return byId;
            }
        }

        if (!hasText(buyerProductName)) {
            return null;
        }

        return adminProductRepository.findByTitleContainingIgnoreCase(buyerProductName.trim()).stream()
                .filter(product -> namesMatch(product.getTitle(), buyerProductName))
                .findFirst()
                .orElse(null);
    }

    private void applyStoreToBuyerProduct(
            com.project.kfpcl_exports.buyer.model.Product buyerProduct,
            StoreAssignment storeAssignment) {
        if (buyerProduct == null || storeAssignment == null) {
            return;
        }

        boolean changed = false;
        if (storeAssignment.storeId() != null
                && !java.util.Objects.equals(buyerProduct.getStoreId(), storeAssignment.storeId())) {
            buyerProduct.setStoreId(storeAssignment.storeId());
            changed = true;
        }
        if (hasText(storeAssignment.storeName())
                && !storeAssignment.storeName().trim().equals(buyerProduct.getStoreName())) {
            buyerProduct.setStoreName(storeAssignment.storeName().trim());
            changed = true;
        }

        if (changed) {
            buyerProductRepository.save(buyerProduct);
        }
    }

    private StoreAssignment resolveRfqStore(Rfq buyerRfq) {
        Long storeId = buyerRfq.getStoreId();
        String storeName = buyerRfq.getStoreName();
        if (storeId == null || !hasText(storeName)) {
            StoreAssignment productStore = resolveProductStore(buyerRfq.getProduct(), null);
            if (storeId == null) {
                storeId = productStore.storeId();
            }
            if (!hasText(storeName)) {
                storeName = productStore.storeName();
            }
        }
        return new StoreAssignment(storeId, storeName);
    }

    private void saveMissingRfqStore(Rfq buyerRfq, StoreAssignment storeAssignment) {
        boolean changed = false;
        if (buyerRfq.getStoreId() == null && storeAssignment.storeId() != null) {
            buyerRfq.setStoreId(storeAssignment.storeId());
            changed = true;
        }
        if (!hasText(buyerRfq.getStoreName()) && hasText(storeAssignment.storeName())) {
            buyerRfq.setStoreName(storeAssignment.storeName().trim());
            changed = true;
        }
        if (changed) {
            rfqRepository.save(buyerRfq);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean namesMatch(String first, String second) {
        return hasText(first) && hasText(second) && first.trim().equalsIgnoreCase(second.trim());
    }

    public void syncToAdminRfqTable(Rfq buyerRfq, String statusStr) {
        try {
            if (buyerRfq == null || buyerRfq.getRfqCode() == null) return;

            StoreAssignment storeAssignment = resolveRfqStore(buyerRfq);
            saveMissingRfqStore(buyerRfq, storeAssignment);

            com.project.kfpcl_exports.admin.model.Rfq adminRfq = adminRfqRepository.findByRfqNumber(buyerRfq.getRfqCode())
                    .orElse(new com.project.kfpcl_exports.admin.model.Rfq());

            adminRfq.setRfqNumber(buyerRfq.getRfqCode());
            adminRfq.setCustomerName(buyerRfq.getBuyerName() != null ? buyerRfq.getBuyerName() : (buyerRfq.getBuyer() != null ? buyerRfq.getBuyer().getName() : "Buyer"));
            adminRfq.setCustomerPhone(buyerRfq.getBuyerPhone() != null ? buyerRfq.getBuyerPhone() : (buyerRfq.getBuyer() != null ? buyerRfq.getBuyer().getPhoneNumber() : null));
            adminRfq.setCustomerEmail(buyerRfq.getBuyer() != null ? buyerRfq.getBuyer().getEmail() : null);

            if (buyerRfq.getProduct() != null) {
                adminRfq.setProductName(buyerRfq.getProduct().getName() != null ? buyerRfq.getProduct().getName() : buyerRfq.getProduct().getTitle());
            }
            // Store is a snapshot from the buyer-selected catalog product.
            if (storeAssignment.storeId() != null) {
                adminRfq.setStoreId(storeAssignment.storeId());
            }
            if (hasText(storeAssignment.storeName())) {
                adminRfq.setStoreName(storeAssignment.storeName().trim());
            }

            if (buyerRfq.getQuantity() != null) {
                String qStr = buyerRfq.getQuantity().trim();
                String numOnly = qStr.replaceAll("[^0-9]", "");
                if (!numOnly.isEmpty()) {
                    try {
                        adminRfq.setQuantity(Integer.parseInt(numOnly));
                    } catch (Exception ignored) {}
                }
                String[] parts = qStr.split("\\s+", 2);
                if (parts.length > 1) {
                    adminRfq.setUnit(parts[1]);
                }
            }

            adminRfq.setDestinationCountry(buyerRfq.getDeliveryLocation());
            adminRfq.setShippingTerms("FOB / Standard");
            adminRfq.setDetails(buyerRfq.getBuyerMessage());
            adminRfq.setStatus(statusStr != null ? statusStr : (buyerRfq.getStatus() != null ? buyerRfq.getStatus().name() : "PENDING"));
            if (adminRfq.getCreatedAt() == null) {
                adminRfq.setCreatedAt(buyerRfq.getCreatedAt() != null ? buyerRfq.getCreatedAt() : LocalDateTime.now());
            }

            adminRfqRepository.save(adminRfq);
        } catch (Exception e) {
            log.warn("Notice: Could not sync RFQ to admin_rfqs: {}", e.getMessage());
        }
    }

    private String sanitizeAndExtractMessage(BuyerCreateRfqRequest request, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return rawMessage;

        String cleanMsg = rawMessage;

        // Extract "Buyer Name: <value>"
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("(?i)Buyer\\s*Name\\s*:\\s*([^\\n]+?)(?=(?:Buyer\\s*Mobile|Mobile|Phone|Subject|Date|$))").matcher(cleanMsg);
        if (nameMatcher.find()) {
            String extractedName = nameMatcher.group(1).trim();
            if ((request.getBuyerName() == null || request.getBuyerName().isBlank()) && !extractedName.isBlank()) {
                request.setBuyerName(extractedName);
            }
            cleanMsg = cleanMsg.replace(nameMatcher.group(0), "").trim();
        }

        // Extract "Buyer Mobile: <value>" or "Mobile: <value>"
        java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile("(?i)(?:Buyer\\s*Mobile|Mobile|Phone)\\s*:\\s*([^\\n]+?)(?=(?:Subject|Date|Buyer\\s*Name|$))").matcher(cleanMsg);
        if (phoneMatcher.find()) {
            String extractedPhone = phoneMatcher.group(1).trim();
            if ((request.getBuyerPhone() == null || request.getBuyerPhone().isBlank()) && !extractedPhone.isBlank()) {
                request.setBuyerPhone(extractedPhone);
            }
            cleanMsg = cleanMsg.replace(phoneMatcher.group(0), "").trim();
        }

        // Extract "Subject: <value>"
        java.util.regex.Matcher subjectMatcher = java.util.regex.Pattern.compile("(?i)Subject\\s*:\\s*([^\\n]+?)(?=(?:Message|Notes|Requirement|Date|$))").matcher(cleanMsg);
        if (subjectMatcher.find()) {
            String extractedSubject = subjectMatcher.group(1).trim();
            if ((request.getSubject() == null || request.getSubject().isBlank()) && !extractedSubject.isBlank()) {
                request.setSubject(extractedSubject);
            }
            cleanMsg = cleanMsg.replace(subjectMatcher.group(0), "").trim();
        }

        // Remove lingering labels like "Message:" or "Notes:"
        cleanMsg = cleanMsg.replaceAll("(?i)^(?:Message|Notes|Requirement|Details)\\s*:\\s*", "").trim();

        return cleanMsg.isBlank() ? (request.getSubject() != null ? request.getSubject() : "Price enquiry") : cleanMsg;
    }
}
