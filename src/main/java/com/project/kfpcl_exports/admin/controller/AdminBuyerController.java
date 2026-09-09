package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/admin/buyers", "/api/admin/buyer-users"})
public class AdminBuyerController {

    private final UserRepository buyerUserRepository;

    public AdminBuyerController(
            @Qualifier("buyerUserRepository") UserRepository buyerUserRepository
    ) {
        this.buyerUserRepository = buyerUserRepository;
    }

    /**
     * Get all buyers from buyer_users table with full KYC & registration details.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBuyers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        List<User> buyers = buyerUserRepository.findAll();

        // 1. Search filter
        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            buyers = buyers.stream()
                    .filter(b -> (b.getFullName() != null && b.getFullName().toLowerCase().contains(query))
                            || (b.getPhoneNumber() != null && b.getPhoneNumber().contains(query))
                            || (b.getEmail() != null && b.getEmail().toLowerCase().contains(query))
                            || (b.getCompanyName() != null && b.getCompanyName().toLowerCase().contains(query))
                            || (b.getGstin() != null && b.getGstin().toLowerCase().contains(query))
                            || (b.getPanNumber() != null && b.getPanNumber().toLowerCase().contains(query))
                            || (b.getCity() != null && b.getCity().toLowerCase().contains(query))
                            || (b.getState() != null && b.getState().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        // 2. Status filter
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            String targetStatus = status.trim().toUpperCase();
            buyers = buyers.stream()
                    .filter(b -> b.getStatus() != null && b.getStatus().equalsIgnoreCase(targetStatus))
                    .collect(Collectors.toList());
        }

        // Sort latest first
        buyers.sort((a, b) -> {
            LocalDateTime tA = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime tB = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return tB.compareTo(tA);
        });

        int totalElements = buyers.size();

        if (page == null && size == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", buyers);
            response.put("buyers", buyers);
            response.put("content", buyers);
            response.put("totalElements", totalElements);
            response.put("totalPages", 1);
            response.put("currentPage", 0);
            response.put("pageSize", totalElements);
            return ResponseEntity.ok(response);
        }

        int p = Math.max(0, page != null ? page : 0);
        int s = size != null && size > 0 ? size : 10;
        int totalPages = (int) Math.ceil((double) totalElements / s);
        int fromIndex = Math.min(p * s, totalElements);
        int toIndex = Math.min(fromIndex + s, totalElements);
        List<User> paginatedList = buyers.subList(fromIndex, toIndex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", paginatedList);
        response.put("buyers", paginatedList);
        response.put("content", paginatedList);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", p);
        response.put("pageSize", s);

        return ResponseEntity.ok(response);
    }

    /**
     * Get single buyer details by UUID id or phone number.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBuyerById(@PathVariable String id) {
        Optional<User> buyerOpt = buyerUserRepository.findById(id);
        if (buyerOpt.isEmpty()) {
            buyerOpt = buyerUserRepository.findByPhoneNumber(id);
        }

        if (buyerOpt.isPresent()) {
            User b = buyerOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", b);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Buyer not found with id or phone: " + id
        ));
    }

    /**
     * Update buyer verification status (VERIFIED, REJECTED, ACTIVE, PENDING_VERIFICATION).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBuyerStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload
    ) {
        Optional<User> buyerOpt = buyerUserRepository.findById(id);
        if (buyerOpt.isEmpty()) {
            buyerOpt = buyerUserRepository.findByPhoneNumber(id);
        }

        if (buyerOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Buyer not found with id: " + id
            ));
        }

        User buyer = buyerOpt.get();
        if (payload.containsKey("status")) {
            String newStatus = String.valueOf(payload.get("status")).trim().toUpperCase();
            buyer.setStatus(newStatus);
            if ("VERIFIED".equals(newStatus) || "ACTIVE".equals(newStatus)) {
                buyer.setEnabled(true);
            } else if ("REJECTED".equals(newStatus) || "BLOCKED".equals(newStatus) || "INACTIVE".equals(newStatus)) {
                buyer.setEnabled(false);
            }
        }

        if (payload.containsKey("enabled")) {
            buyer.setEnabled(Boolean.parseBoolean(String.valueOf(payload.get("enabled"))));
        }

        User updated = buyerUserRepository.save(buyer);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Buyer status updated successfully",
                "data", updated
        ));
    }

    /**
     * Delete a buyer by UUID id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBuyer(@PathVariable String id) {
        if (!buyerUserRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Buyer not found with id: " + id
            ));
        }

        buyerUserRepository.deleteById(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Buyer deleted successfully"
        ));
    }
}
