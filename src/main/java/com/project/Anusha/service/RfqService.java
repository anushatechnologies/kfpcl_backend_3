package com.project.Anusha.service;

import com.project.Anusha.dto.*;
import com.project.Anusha.enums.NotificationType;
import com.project.Anusha.enums.RfqStatus;
import com.project.Anusha.exception.RfqException;
import com.project.Anusha.model.*;
import com.project.Anusha.repository.ProductRepository;
import com.project.Anusha.repository.RfqRepository;
import com.project.Anusha.repository.RfqResponseRepository;
import com.project.Anusha.util.RfqCodeGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class RfqService {

    private final RfqRepository rfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final ProductRepository productRepository;
    private final RfqCodeGenerator rfqCodeGenerator;
    private final NotificationService notificationService;

    public RfqService(
            RfqRepository rfqRepository,
            RfqResponseRepository rfqResponseRepository,
            ProductRepository productRepository,
            RfqCodeGenerator rfqCodeGenerator,
            NotificationService notificationService
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqResponseRepository = rfqResponseRepository;
        this.productRepository = productRepository;
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
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> RfqException.productNotFound("Product not found with id: " + request.getProductId()));

        if (Boolean.FALSE.equals(product.getIsActive())) {
            throw RfqException.invalidRequest("Product is currently not active");
        }

        String rfqCode = rfqCodeGenerator.generateRfqCode();
        LocalDateTime now = LocalDateTime.now();

        Rfq rfq = Rfq.builder()
                .rfqCode(rfqCode)
                .buyer(buyer)
                .product(product)
                .quantity(request.getQuantity())
                .deliveryLocation(request.getDeliveryLocation())
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
        if (status != null) {
            page = rfqRepository.findByBuyerAndStatusOrderByCreatedAtDesc(buyer, status, pageable);
        } else {
            page = rfqRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable);
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
        notificationService.createNotification(
                buyer,
                NotificationType.RFQ_ACCEPTED,
                "RFQ Accepted",
                "You have successfully accepted the quotation for RFQ " + saved.getRfqCode() + ". Contact details are now accessible.",
                "RFQ",
                saved.getRfqCode()
        );

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
        notificationService.createNotification(
                buyer,
                NotificationType.RFQ_REJECTED,
                "RFQ Rejected",
                "You have rejected the quotation for RFQ " + saved.getRfqCode() + ".",
                "RFQ",
                saved.getRfqCode()
        );

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
                .contactName(latestResponse.getContactName())
                .contactPhone(latestResponse.getContactPhone())
                .contactEmail(latestResponse.getContactEmail())
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

        notificationService.createNotification(
                rfq.getBuyer(),
                NotificationType.RFQ_RESPONSE_RECEIVED,
                "RFQ Response Received",
                "A response has been received for RFQ " + rfq.getRfqCode() + ".",
                "RFQ",
                rfq.getRfqCode()
        );
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Rfq findRfqAndValidateOwnership(String rfqIdOrCode, User buyer) {
        Rfq rfq = findRfqByIdOrCode(rfqIdOrCode);
        if (!rfq.getBuyer().getId().equals(buyer.getId())) {
            throw RfqException.accessDenied("Access denied: RFQ does not belong to you");
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
        if (rfq.getProduct() != null) {
            productDto = BuyerRfqResponseDto.ProductSummaryDto.builder()
                    .id(rfq.getProduct().getId())
                    .name(rfq.getProduct().getName())
                    .description(rfq.getProduct().getDescription())
                    .imageUrl(rfq.getProduct().getImageUrl())
                    .build();
        }

        BuyerRfqResponseDto.RfqResponseSummaryDto responseDto = null;
        RfqResponse latest = rfq.getLatestResponse();
        if (latest != null) {
            responseDto = BuyerRfqResponseDto.RfqResponseSummaryDto.builder()
                    .quotedPrice(latest.getQuotedPrice())
                    .availableQuantity(latest.getAvailableQuantity())
                    .deliveryTime(latest.getDeliveryTime())
                    .responseMessage(latest.getResponseMessage())
                    .createdAt(latest.getCreatedAt())
                    .build();
        }

        return BuyerRfqResponseDto.builder()
                .id(rfq.getId())
                .rfqId(rfq.getId().toString())
                .rfqCode(rfq.getRfqCode())
                .product(productDto)
                .quantity(rfq.getQuantity())
                .deliveryLocation(rfq.getDeliveryLocation())
                .buyerMessage(rfq.getBuyerMessage())
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
