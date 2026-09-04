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
public class RfqController {

    private static final Logger log = LoggerFactory.getLogger(RfqController.class);

    private final RfqRepository buyerRfqRepository;
    private final RfqResponseRepository rfqResponseRepository;

    private Map<String, Object> mapRfqToMap(Rfq rfq) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rfq.getId());
        map.put("rfqId", String.valueOf(rfq.getId()));
        map.put("rfqCode", rfq.getRfqCode());
        map.put("quantity", rfq.getQuantity());
        map.put("deliveryLocation", rfq.getDeliveryLocation());
        map.put("buyerMessage", rfq.getBuyerMessage());
        map.put("status", rfq.getStatus() != null ? rfq.getStatus().name() : "SUBMITTED");
        map.put("createdAt", rfq.getCreatedAt());

        if (rfq.getBuyer() != null) {
            map.put("buyerId", rfq.getBuyer().getId());
            map.put("buyerName", rfq.getBuyer().getName());
            map.put("buyerEmail", rfq.getBuyer().getEmail());
            map.put("buyerPhone", rfq.getBuyer().getPhoneNumber());
            map.put("userEmail", rfq.getBuyer().getEmail());
        }

        if (rfq.getProduct() != null) {
            map.put("productId", rfq.getProduct().getId());
            map.put("productTitle", rfq.getProduct().getTitle());
            map.put("productName", rfq.getProduct().getTitle());
            map.put("productImage", rfq.getProduct().getMainImageUrl());
            map.put("mainImageUrl", rfq.getProduct().getMainImageUrl());
            map.put("price", rfq.getProduct().getPrice());
        }

        if (rfq.getLatestResponse() != null) {
            RfqResponse resp = rfq.getLatestResponse();
            Map<String, Object> respMap = new HashMap<>();
            respMap.put("quotedPrice", resp.getQuotedPrice());
            respMap.put("availableQuantity", resp.getAvailableQuantity());
            respMap.put("deliveryTime", resp.getDeliveryTime());
            respMap.put("responseMessage", resp.getResponseMessage());
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

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", contentList);
        responseMap.put("totalPages", pageResult.getTotalPages());
        responseMap.put("totalElements", pageResult.getTotalElements());
        responseMap.put("number", pageResult.getNumber());
        responseMap.put("size", pageResult.getSize());
        responseMap.put("success", true);

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
    @RequestMapping(value = {"/{rfqId}/quotation", "/{rfqId}/respond"}, method = {RequestMethod.POST, RequestMethod.PUT})
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
        String quantityStr = (request.getQuantity() != null) ? String.valueOf(request.getQuantity()) : rfq.getQuantity();
        String deliveryDays = (request.getDeliveryDays() != null) ? request.getDeliveryDays() : "3-5 days";
        String notes = (request.getNotes() != null) ? request.getNotes() : "Quotation provided by KFPCL admin";

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

        rfqResponseRepository.save(response);
        rfq.getResponses().add(response);
        rfq.setStatus(RfqStatus.RESPONDED);
        rfq.setUpdatedAt(LocalDateTime.now());
        buyerRfqRepository.save(rfq);

        return ResponseEntity.ok(Map.of(
                "message", "Quotation successfully created and sent to buyer",
                "success", true,
                "rfqId", rfq.getId(),
                "rfqCode", rfq.getRfqCode(),
                "quotedPrice", unitPrice,
                "availableQuantity", quantityStr,
                "deliveryDays", deliveryDays,
                "notes", notes
        ));
    }
}
