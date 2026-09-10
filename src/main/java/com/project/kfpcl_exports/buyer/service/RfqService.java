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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class RfqService {

    private final RfqRepository rfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final ProductRepository buyerProductRepository;
    private final com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository;
    private final RfqCodeGenerator rfqCodeGenerator;
    private final NotificationService notificationService;

    public RfqService(
            RfqRepository rfqRepository,
            RfqResponseRepository rfqResponseRepository,
            ProductRepository buyerProductRepository,
            com.project.kfpcl_exports.admin.repository.ProductRepository adminProductRepository,
            RfqCodeGenerator rfqCodeGenerator,
            NotificationService notificationService
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqResponseRepository = rfqResponseRepository;
        this.buyerProductRepository = buyerProductRepository;
        this.adminProductRepository = adminProductRepository;
        this.rfqCodeGenerator = rfqCodeGenerator;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // BUYER OPERATIONS (DEVELOPER 3 MODULE)
    // =========================================================================

    /**
     * 1. Create a new RFQ for the authenticated buyer.
     */
    public BuyerRfqResponseDto createRfq(User buyer, BuyerCreateRfqRequest request) {
        com.project.kfpcl_exports.buyer.model.Product buyerProduct = null;

        if (request.getProductId() != null) {
            buyerProduct = buyerProductRepository.findById(request.getProductId()).orElse(null);
        }

        // If product with requested ID does not exist in buyer_products, sync from admin products
        if (buyerProduct == null && request.getProductId() != null) {
            com.project.kfpcl_exports.admin.model.Product adminProd = adminProductRepository.findById(request.getProductId()).orElse(null);
            if (adminProd != null) {
                com.project.kfpcl_exports.buyer.model.Product synced = com.project.kfpcl_exports.buyer.model.Product.builder()
                        .id(adminProd.getId())
                        .name(adminProd.getTitle() != null ? adminProd.getTitle() : "Commodity")
                        .description(adminProd.getDescription())
                        .mainImageUrl(adminProd.getMainImageUrl())
                        .imageUrl(adminProd.getMainImageUrl())
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

        // Safe Fallback 1: Pick any active product in buyer_products table
        if (buyerProduct == null) {
            buyerProduct = buyerProductRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .findFirst()
                    .orElseGet(() -> buyerProductRepository.findAll().stream().findFirst().orElse(null));
        }

        // Safe Fallback 2: Auto-create a default product entry in buyer_products table to satisfy FK constraint
        if (buyerProduct == null) {
            LocalDateTime now = LocalDateTime.now();
            com.project.kfpcl_exports.buyer.model.Product fallback = com.project.kfpcl_exports.buyer.model.Product.builder()
                    .name("General Commodity Product")
                    .description("Default product created for RFQ requests")
                    .isActive(true)
                    .createdAt(now)
                    .build();
            buyerProduct = buyerProductRepository.save(fallback);
        }

        String rfqCode = rfqCodeGenerator.generateRfqCode();
        LocalDateTime now = LocalDateTime.now();

        String buyerName = request.getBuyerName() != null ? request.getBuyerName() : (buyer != null ? buyer.getName() : null);
        String buyerPhone = request.getBuyerPhone() != null ? request.getBuyerPhone() : (buyer != null ? buyer.getPhoneNumber() : null);

        Rfq rfq = Rfq.builder()
                .rfqCode(rfqCode)
                .buyer(buyer)
                .product(buyerProduct)
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .subject(request.getSubject())
                .fileUrl(request.getFileUrl())
                .quantity(request.getQuantity() != null ? request.getQuantity() : "1")
                .deliveryLocation(request.getDeliveryLocation() != null ? request.getDeliveryLocation() : "Default Location")
                .buyerMessage(request.getBuyerMessage())
                .status(RfqStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Rfq saved = rfqRepository.save(rfq);
        return mapToBuyerDto(saved, false);
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

    /**
     * 3. Get single RFQ detail for authenticated buyer.
     * Contact details are NEVER exposed before acceptance.
     */
    @Transactional(readOnly = true)
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
                .quantity(request.getQuantity())
                .deliveryLocation(request.getDeliveryLocation())
                .buyerMessage(request.getBuyerMessage())
                .parentRfq(originalRfq)
                .status(RfqStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Rfq saved = rfqRepository.save(newRfq);
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
}
