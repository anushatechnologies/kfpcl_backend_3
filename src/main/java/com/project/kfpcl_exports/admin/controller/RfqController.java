package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.dto.QuotationRequest;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import com.project.kfpcl_exports.buyer.model.Rfq;
import com.project.kfpcl_exports.buyer.model.RfqResponse;
import com.project.kfpcl_exports.buyer.repository.RfqRepository;
import com.project.kfpcl_exports.buyer.repository.RfqResponseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController("adminRfqController")
@RequestMapping({"/api/admin/rfqs", "/api/rfqs/admin"})
@RequiredArgsConstructor
@Transactional
public class RfqController {

    private static final Logger log = LoggerFactory.getLogger(RfqController.class);

    private final RfqRepository buyerRfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final com.project.kfpcl_exports.buyer.service.NotificationService notificationService;

    private Map<String, Object> mapRfqToMap(Rfq rfq) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rfq.getId());
        map.put("rfqId", String.valueOf(rfq.getId()));
        map.put("rfqCode", rfq.getRfqCode());
        map.put("quantity", rfq.getQuantity());
        map.put("deliveryLocation", rfq.getDeliveryLocation());
        map.put("buyerMessage", rfq.getBuyerMessage());
        map.put("subject", rfq.getSubject());
        map.put("fileUrl", rfq.getFileUrl());
        map.put("status", rfq.getStatus() != null ? rfq.getStatus().name() : "SUBMITTED");
        map.put("createdAt", rfq.getCreatedAt());

        String buyerName = rfq.getBuyerName();
        String buyerPhone = rfq.getBuyerPhone();
        String buyerId = null;
        String buyerEmail = null;

        try {
            com.project.kfpcl_exports.buyer.model.User buyer = rfq.getBuyer();
            if (buyer != null) {
                buyerId = buyer.getId();
                buyerEmail = buyer.getEmail();
                if (buyerName == null || buyerName.isBlank()) {
                    buyerName = buyer.getName();
                }
                if (buyerPhone == null || buyerPhone.isBlank()) {
                    buyerPhone = buyer.getPhoneNumber();
                }
            }
        } catch (Exception e) {
            log.warn("Could not load buyer for RFQ id {}: {}", rfq.getId(), e.getMessage());
        }

        map.put("buyerName", buyerName);
        map.put("buyerPhone", buyerPhone);
        if (buyerId != null) {
            map.put("buyerId", buyerId);
            map.put("buyerEmail", buyerEmail);
            map.put("userEmail", buyerEmail);
        }

        try {
            if (rfq.getProduct() != null) {
                String pName = rfq.getProduct().getName() != null ? rfq.getProduct().getName() : rfq.getProduct().getTitle();
                map.put("productId", rfq.getProduct().getId());
                map.put("productTitle", pName);
                map.put("productName", pName);
                map.put("productImage", rfq.getProduct().getMainImageUrl());
                map.put("mainImageUrl", rfq.getProduct().getMainImageUrl());
                map.put("price", rfq.getProduct().getIndicativePrice());
            }
        } catch (Exception e) {
            log.warn("Could not load product for RFQ id {}: {}", rfq.getId(), e.getMessage());
        }

        if (rfq.getLatestResponse() != null) {
            RfqResponse resp = rfq.getLatestResponse();
            Map<String, Object> respMap = new HashMap<>();
            respMap.put("quoteId", "QUO-" + resp.getId());
            respMap.put("quotedPrice", resp.getQuotedPrice());
            respMap.put("offeredPrice", resp.getQuotedPrice());
            respMap.put("availableQuantity", resp.getAvailableQuantity());
            respMap.put("deliveryTime", resp.getDeliveryTime());
            respMap.put("leadTime", resp.getDeliveryTime());
            respMap.put("responseMessage", resp.getResponseMessage());
            respMap.put("notes", resp.getResponseMessage());
            respMap.put("createdAt", resp.getCreatedAt());
            map.put("quotation", respMap);
            map.put("response", respMap);
        }

        return map;
    }

    @GetMapping
    public ResponseEntity<?> getRfqs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 20, Sort.by("createdAt").descending());
        Page<Rfq> pageResult;
        if (status != null && !status.isBlank()) {
            try {
                RfqStatus st = RfqStatus.valueOf(status.toUpperCase());
                pageResult = buyerRfqRepository.findByStatusOrderByCreatedAtDesc(st, pageable);
            } catch (Exception e) {
                pageResult = buyerRfqRepository.findAll(pageable);
            }
        } else {
            pageResult = buyerRfqRepository.findAll(pageable);
        }

        List<Map<String, Object>> contentList = pageResult.getContent().stream()
                .map(this::mapRfqToMap)
                .collect(Collectors.toList());

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("rfqs", contentList);
        dataMap.put("totalPages", pageResult.getTotalPages());
        dataMap.put("totalElements", pageResult.getTotalElements());
        dataMap.put("currentPage", pageResult.getNumber());
        dataMap.put("pageSize", pageResult.getSize());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("data", dataMap);
        responseMap.put("content", contentList);
        responseMap.put("totalPages", pageResult.getTotalPages());
        responseMap.put("totalElements", pageResult.getTotalElements());
        responseMap.put("number", pageResult.getNumber());
        responseMap.put("size", pageResult.getSize());

        return ResponseEntity.ok(responseMap);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRfqById(@PathVariable String id) {
        Optional<Rfq> rfqOpt = Optional.empty();
        try {
            Long numericId = Long.parseLong(id);
            rfqOpt = buyerRfqRepository.findById(numericId);
        } catch (NumberFormatException ignored) {}

        if (rfqOpt.isEmpty()) {
            rfqOpt = buyerRfqRepository.findByRfqCode(id);
        }

        if (rfqOpt.isPresent()) {
            return ResponseEntity.ok(mapRfqToMap(rfqOpt.get()));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Submit Quotation for RFQ by Admin
     * POST /api/admin/rfqs/{rfqId}/quotation or POST /api/admin/rfqs/{rfqId}/respond
     */
    @RequestMapping(value = {"/{rfqId}/quotation", "/{rfqId}/quotations", "/{rfqId}/quote", "/{rfqId}/respond"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> submitQuotation(
            @PathVariable String rfqId,
            @RequestBody QuotationRequest request) {

        Optional<Rfq> rfqOpt = Optional.empty();
        try {
            Long numericId = Long.parseLong(rfqId);
            rfqOpt = buyerRfqRepository.findById(numericId);
        } catch (NumberFormatException ignored) {}

        if (rfqOpt.isEmpty()) {
            rfqOpt = buyerRfqRepository.findByRfqCode(rfqId);
        }

        if (rfqOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "RFQ not found with ID: " + rfqId,
                    "success", false
            ));
        }

        Rfq rfq = rfqOpt.get();

        Double unitPrice = (request.getUnitPrice() != null) ? request.getUnitPrice() : 0.0;
        int qty = (request.getQuantity() != null) ? request.getQuantity() : 1;
        String quantityStr = (request.getQuantity() != null) ? String.valueOf(request.getQuantity()) : rfq.getQuantity();
        String deliveryDays = (request.getDeliveryDays() != null) ? request.getDeliveryDays() : "5 days";

        StringBuilder noteBuilder = new StringBuilder();
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            noteBuilder.append(request.getNotes().trim());
        }
        if (request.getPaymentTerms() != null && !request.getPaymentTerms().isBlank()) {
            if (noteBuilder.length() > 0) noteBuilder.append(" | ");
            noteBuilder.append("Payment Terms: ").append(request.getPaymentTerms().trim());
        }
        if (request.getAvailability() != null && !request.getAvailability().isBlank()) {
            if (noteBuilder.length() > 0) noteBuilder.append(" | ");
            noteBuilder.append("Availability: ").append(request.getAvailability().trim());
        }
        if (request.getMoq() != null) {
            if (noteBuilder.length() > 0) noteBuilder.append(" | ");
            noteBuilder.append("MOQ: ").append(request.getMoq());
        }
        String notes = noteBuilder.length() > 0 ? noteBuilder.toString() : "Price includes GST & loading at warehouse.";

        RfqResponse response = RfqResponse.builder()
                .rfq(rfq)
                .quotedPrice(unitPrice)
                .availableQuantity(quantityStr)
                .deliveryTime(deliveryDays)
                .responseMessage(notes)
                .contactName("KFPCL Admin Team")
                .contactPhone("9876543210")
                .contactEmail("admin@kfpclexports.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RfqResponse savedResp = rfqResponseRepository.save(response);
        rfq.getResponses().add(savedResp);
        rfq.setStatus(RfqStatus.RESPONDED);
        rfq.setUpdatedAt(LocalDateTime.now());
        buyerRfqRepository.save(rfq);

        try {
            com.project.kfpcl_exports.buyer.model.User buyer = rfq.getBuyer();
            if (buyer != null && notificationService != null) {
                notificationService.createNotification(
                        buyer,
                        com.project.kfpcl_exports.buyer.enums.NotificationType.RFQ_RESPONSE_RECEIVED,
                        "Quotation Received",
                        "Supplier quoted ₹" + unitPrice + " for your enquiry " + rfq.getRfqCode(),
                        "RFQ",
                        rfq.getRfqCode()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to create notification for quotation: {}", e.getMessage());
        }

        double totalAmount = unitPrice * qty;
        String quoteId = "QUO-" + (savedResp.getId() != null ? savedResp.getId() : "8841");

        Map<String, Object> quoteData = new HashMap<>();
        quoteData.put("quoteId", quoteId);
        quoteData.put("rfqCode", rfq.getRfqCode());
        quoteData.put("unitPrice", unitPrice);
        quoteData.put("totalAmount", totalAmount);
        quoteData.put("leadTime", deliveryDays);
        quoteData.put("status", "QUOTED");
        quoteData.put("notes", notes);
        quoteData.put("quotedAt", savedResp.getCreatedAt());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Quotation submitted and buyer notified successfully");
        result.put("data", quoteData);
        result.put("rfqId", rfq.getId());
        result.put("rfqCode", rfq.getRfqCode());
        result.put("quotedPrice", unitPrice);
        result.put("totalAmount", totalAmount);
        result.put("availableQuantity", quantityStr);
        result.put("deliveryDays", deliveryDays);
        result.put("notes", notes);

        return ResponseEntity.ok(result);
    }
}
