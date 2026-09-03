package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.dto.QuotationRequest;
import com.project.kfpcl_exports.admin.model.Quotation;
import com.project.kfpcl_exports.admin.model.Rfq;
import com.project.kfpcl_exports.admin.repository.QuotationRepository;
import com.project.kfpcl_exports.admin.repository.RfqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/rfqs")
@RequiredArgsConstructor
public class RfqController {

    private final RfqRepository rfqRepository;
    private final QuotationRepository quotationRepository;

    @GetMapping
    public ResponseEntity<Page<Rfq>> getRfqs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(rfqRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rfq> getRfqById(@PathVariable Long id) {
        return rfqRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * CRITICAL API: Submit Quotation for RFQ
     * POST /api/admin/rfqs/{rfqId}/quotation
     * Payload:
     * {
     *   "unitPrice": 1200,
     *   "quantity": 100,
     *   "deliveryDays": "7 days",
     *   "notes": "Quotation valid for 15 days"
     * }
     */
    @PostMapping("/{rfqId}/quotation")
    public ResponseEntity<Map<String, Object>> submitQuotation(
            @PathVariable Long rfqId,
            @RequestBody QuotationRequest request) {

        Optional<Rfq> rfqOpt = rfqRepository.findById(rfqId);
        if (rfqOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "RFQ not found with ID: " + rfqId,
                    "success", false
            ));
        }

        Rfq rfq = rfqOpt.get();

        // Check if quotation already exists for this RFQ, or create new one
        Quotation quotation = quotationRepository.findByRfqId(rfqId).orElseGet(Quotation::new);
        quotation.setUnitPrice(request.getUnitPrice());
        quotation.setQuantity(request.getQuantity());
        quotation.setDeliveryDays(request.getDeliveryDays());
        quotation.setNotes(request.getNotes());
        quotation.setRfq(rfq);

        Quotation savedQuotation = quotationRepository.save(quotation);

        // Update RFQ status to QUOTED and attach quotation
        rfq.setStatus("QUOTED");
        rfq.setQuotation(savedQuotation);
        rfqRepository.save(rfq);

        return ResponseEntity.ok(Map.of(
                "message", "Quotation successfully created and sent to buyer",
                "success", true,
                "rfqId", rfqId,
                "quotationId", savedQuotation.getId(),
                "unitPrice", savedQuotation.getUnitPrice(),
                "quantity", savedQuotation.getQuantity(),
                "totalPrice", savedQuotation.getTotalPrice(),
                "deliveryDays", savedQuotation.getDeliveryDays(),
                "notes", savedQuotation.getNotes()
        ));
    }
}
