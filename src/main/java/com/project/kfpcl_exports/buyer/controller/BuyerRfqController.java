package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.*;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.service.RfqService;
import com.project.kfpcl_exports.buyer.util.BuyerAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/buyer/rfqs")
public class BuyerRfqController {

    private final RfqService rfqService;
    private final BuyerAuthHelper buyerAuthHelper;

    public BuyerRfqController(RfqService rfqService, BuyerAuthHelper buyerAuthHelper) {
        this.rfqService = rfqService;
        this.buyerAuthHelper = buyerAuthHelper;
    }

    /**
     * 1. CREATE RFQ
     * POST /api/buyer/rfqs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> createRfq(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BuyerCreateRfqRequest request,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        BuyerRfqResponseDto response = rfqService.createRfq(buyer, request);
        return new ResponseEntity<>(ApiResponse.ok("RFQ created successfully", response), HttpStatus.CREATED);
    }

    /**
     * 2. GET BUYER RFQs
     * GET /api/buyer/rfqs
     */
    @GetMapping({"", "/buyer/{buyerId}"})
    public ResponseEntity<ApiResponse<Page<BuyerRfqResponseDto>>> getBuyerRfqs(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) RfqStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        Page<BuyerRfqResponseDto> responses = rfqService.getBuyerRfqs(buyer, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Buyer RFQs fetched successfully", responses));
    }

    /**
     * 3. GET RFQ DETAILS
     * GET /api/buyer/rfqs/{rfqId}
     */
    @GetMapping("/{rfqId}")
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> getRfqDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        BuyerRfqResponseDto response = rfqService.getBuyerRfqDetail(buyer, rfqId);
        return ResponseEntity.ok(ApiResponse.ok("RFQ details fetched successfully", response));
    }

    /**
     * 4. ACCEPT ADMIN RESPONSE
     * POST /api/buyer/rfqs/{rfqId}/accept
     */
    @PostMapping({"/{rfqId}/accept", "/{rfqId}/respond"})
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> acceptRfqResponse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        BuyerRfqResponseDto response = rfqService.acceptRfqResponse(buyer, rfqId);
        return ResponseEntity.ok(ApiResponse.ok("RFQ response accepted successfully", response));
    }

    /**
     * 5. REJECT ADMIN RESPONSE / UNIFIED RESPOND
     * POST /api/buyer/rfqs/{rfqId}/reject
     * POST /api/buyer/rfqs/{rfqId}/respond (if action = REJECT in body, else ACCEPT)
     */
    @PostMapping({"/{rfqId}/reject", "/{rfqId}/respond"})
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> respondToRfq(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        
        // Determine if this is an ACCEPT or REJECT action
        boolean isAccept = false;
        String reason = null;
        
        if (request != null) {
            String action = request.get("action");
            if ("ACCEPT".equalsIgnoreCase(action)) {
                isAccept = true;
            } else if ("REJECT".equalsIgnoreCase(action)) {
                isAccept = false;
                reason = request.get("reason");
            } else if (request.containsKey("reason")) {
                reason = request.get("reason");
            }
        }
        
        // Note: The /accept endpoint handles strict accept. If someone calls /respond without action, we default to reject to be safe unless action=ACCEPT.
        if (isAccept) {
            BuyerRfqResponseDto response = rfqService.acceptRfqResponse(buyer, rfqId);
            return ResponseEntity.ok(ApiResponse.ok("RFQ response accepted successfully", response));
        } else {
            BuyerRfqResponseDto response = rfqService.rejectRfqResponse(buyer, rfqId, reason);
            return ResponseEntity.ok(ApiResponse.ok("RFQ response rejected", response));
        }
    }

    /**
     * 6. RE-RAISE RFQ
     * POST /api/buyer/rfqs/{rfqId}/re-raise
     */
    @PostMapping({"/{rfqId}/re-raise", "/{rfqId}/reraise"})
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> reRaiseRfq(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            @Valid @RequestBody BuyerReRaiseRfqRequest request,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        BuyerRfqResponseDto response = rfqService.reRaiseRfq(buyer, rfqId, request);
        return new ResponseEntity<>(ApiResponse.ok("New RFQ raised successfully", response), HttpStatus.CREATED);
    }

    /**
     * 7. GET CONTACT
     * GET /api/buyer/rfqs/{rfqId}/contact
     * SECURITY CRITICAL: Allowed ONLY if RFQ status == ACCEPTED.
     */
    @GetMapping("/{rfqId}/contact")
    public ResponseEntity<ApiResponse<ContactResponseDto>> getContactDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        ContactResponseDto response = rfqService.getContactDetails(buyer, rfqId);
        return ResponseEntity.ok(ApiResponse.ok("Contact details retrieved successfully", response));
    }
}
