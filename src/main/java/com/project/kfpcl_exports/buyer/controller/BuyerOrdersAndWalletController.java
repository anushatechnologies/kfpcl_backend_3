package com.project.kfpcl_exports.buyer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class BuyerOrdersAndWalletController {

    @GetMapping({"/api/buyer/orders", "/api/orders", "/orders"})
    public ResponseEntity<Map<String, Object>> getOrders() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "orders", Collections.emptyList(),
                "data", Collections.emptyList(),
                "total", 0
        ));
    }

    @GetMapping({"/api/wallet/balance/{id}", "/wallet/balance/{id}"})
    public ResponseEntity<Map<String, Object>> getWalletBalance(@PathVariable(value = "id", required = false) String id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", 0.00,
                "currency", "INR",
                "userId", id != null ? id : ""
        ));
    }
}
