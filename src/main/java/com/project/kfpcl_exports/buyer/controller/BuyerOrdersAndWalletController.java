package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.util.BuyerAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class BuyerOrdersAndWalletController {

    private final BuyerAuthHelper buyerAuthHelper;

    public BuyerOrdersAndWalletController(BuyerAuthHelper buyerAuthHelper) {
        this.buyerAuthHelper = buyerAuthHelper;
    }

    @GetMapping({"/api/buyer/orders", "/api/orders", "/orders"})
    public ResponseEntity<Map<String, Object>> getOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        if (buyer == null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orders", Collections.emptyList(),
                    "data", Collections.emptyList(),
                    "total", 0
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "orders", Collections.emptyList(),
                "data", Collections.emptyList(),
                "total", 0
        ));
    }

    @GetMapping({"/api/wallet/balance/{id}", "/wallet/balance/{id}"})
    public ResponseEntity<Map<String, Object>> getWalletBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id", required = false) String id,
            HttpServletRequest httpRequest
    ) {
        User buyer = buyerAuthHelper.resolveAuthenticatedBuyer(userDetails, httpRequest);
        String userId = buyer != null ? buyer.getId() : (id != null ? id : "");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", 0.00,
                "currency", "INR",
                "userId", userId
        ));
    }
}
