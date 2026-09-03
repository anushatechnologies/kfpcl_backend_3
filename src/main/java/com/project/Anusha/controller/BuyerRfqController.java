package com.project.Anusha.controller;

import com.project.Anusha.dto.*;
import com.project.Anusha.enums.RfqStatus;
import com.project.Anusha.model.User;
import com.project.Anusha.service.RfqService;
import com.project.Anusha.util.BuyerAuthHelper;
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

@RestController
@RequestMapping("/api/buyer/rfqs")
@CrossOrigin(origins = "*")
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
    @GetMapping
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
    @PostMapping("/{rfqId}/accept")
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
     * 5. REJECT ADMIN RESPONSE
     * POST /api/buyer/rfqs/{rfqId}/reject
     */
    @PostMapping("/{rfqId}/reject")
    public ResponseEntity<ApiResponse<BuyerRfqResponseDto>> rejectRfqResponse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String rfqId,
            @RequestBody(required = false) BuyerRejectRfqRequest request,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        String reason = request != null ? request.getReason() : null;
        BuyerRfqResponseDto response = rfqService.rejectRfqResponse(buyer, rfqId, reason);
        return ResponseEntity.ok(ApiResponse.ok("RFQ response rejected", response));
    }

    /**
     * 6. RE-RAISE RFQ
     * POST /api/buyer/rfqs/{rfqId}/re-raise
     */
    @PostMapping("/{rfqId}/re-raise")
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
