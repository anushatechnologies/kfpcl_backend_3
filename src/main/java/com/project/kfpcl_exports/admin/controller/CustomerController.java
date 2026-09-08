package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.model.Customer;
import com.project.kfpcl_exports.admin.repository.CustomerRepository;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository;
    private final JdbcTemplate jdbcTemplate;

    public CustomerController(
            CustomerRepository customerRepository,
            @Qualifier("mainUserRepository") UserRepository userRepository,
            @Qualifier("buyerUserRepository") com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.buyerUserRepository = buyerUserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        List<Customer> result = new ArrayList<>(customerRepository.findAll());
        Set<String> existingPhones = result.stream()
                .map(c -> c.getPhone() != null ? c.getPhone().replaceAll("[^0-9]", "") : "")
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toSet());
        Set<String> existingEmails = result.stream()
                .map(c -> c.getEmail() != null ? c.getEmail().trim().toLowerCase() : "")
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());

        // 1. Include registered buyers from users table
        List<User> users = userRepository.findAll();
        for (User u : users) {
            String cleanPhone = u.getPhoneNumber() != null ? u.getPhoneNumber().replaceAll("[^0-9]", "") : "";
            if (!cleanPhone.isEmpty() && existingPhones.contains(cleanPhone)) {
                continue;
            }
            if (u.getEmail() != null && !u.getEmail().isBlank() && existingEmails.contains(u.getEmail().trim().toLowerCase())) {
                continue;
            }
            Customer c = Customer.builder()
                    .id(u.getId())
                    .name(u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Buyer " + cleanPhone)
                    .email(u.getEmail() != null ? u.getEmail() : "")
                    .phone(u.getPhoneNumber())
                    .companyName(u.getCompanyName() != null ? u.getCompanyName() : "KFPCL Buyer")
                    .country(u.getCity() != null && !u.getCity().isBlank() ? u.getCity() + ", " + (u.getState() != null ? u.getState() : "India") : "India")
                    .status(Boolean.TRUE.equals(u.getIsActive()) ? "ACTIVE" : "INACTIVE")
                    .createdAt(u.getCreatedAt())
                    .build();
            result.add(c);
            if (!cleanPhone.isEmpty()) existingPhones.add(cleanPhone);
            if (u.getEmail() != null && !u.getEmail().isBlank()) existingEmails.add(u.getEmail().trim().toLowerCase());
        }

        // 2. Include registered buyers from buyer_users table
        try {
            List<com.project.kfpcl_exports.buyer.model.User> buyerUsers = buyerUserRepository.findAll();
            for (com.project.kfpcl_exports.buyer.model.User bu : buyerUsers) {
                String buPhone = bu.getPhoneNumber();
                String cleanPhone = buPhone != null ? buPhone.replaceAll("[^0-9]", "") : "";
                if (cleanPhone.length() > 10) {
                    cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
                }
                String buEmail = bu.getEmail() != null ? bu.getEmail().trim().toLowerCase() : "";

                if (cleanPhone.isEmpty() && !buEmail.isEmpty()) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{10}").matcher(buEmail);
                    if (m.find()) cleanPhone = m.group();
                }
                if (cleanPhone.isEmpty() && bu.getName() != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{10}").matcher(bu.getName());
                    if (m.find()) cleanPhone = m.group();
                }

                if (!cleanPhone.isEmpty() && existingPhones.contains(cleanPhone)) {
                    continue;
                }
                if (!buEmail.isEmpty() && existingEmails.contains(buEmail)) {
                    continue;
                }

                Long customerId = null;
                Optional<User> uOpt = Optional.empty();
                if (!cleanPhone.isEmpty()) {
                    uOpt = userRepository.findByPhoneNumber(cleanPhone);
                    if (uOpt.isEmpty()) uOpt = userRepository.findByPhoneNumber("+91" + cleanPhone);
                }
                if (uOpt.isEmpty() && !buEmail.isEmpty()) {
                    uOpt = userRepository.findByEmail(buEmail);
                }

                if (uOpt.isPresent()) {
                    customerId = uOpt.get().getId();
                } else {
                    try {
                        User nu = new User();
                        nu.setPhoneNumber(!cleanPhone.isEmpty() ? cleanPhone : "9" + String.format("%09d", Math.abs((long) bu.getId().hashCode() % 1000000000L)));
                        nu.setFullName(bu.getName() != null && !bu.getName().isBlank() ? bu.getName() : "Buyer " + (!cleanPhone.isEmpty() ? cleanPhone : ""));
                        nu.setEmail(bu.getEmail());
                        nu.setCompanyName("KFPCL Buyer");
                        nu.setBusinessType("Buyer");
                        nu.setState("India");
                        nu.setCity("India");
                        nu.setIsActive(bu.isEnabled());
                        nu.setEnabled(bu.isEnabled());
                        nu.setIsVerified(true);
                        nu.setRole("ROLE_BUYER");
                        User saved = userRepository.save(nu);
                        customerId = saved.getId();
                    } catch (Exception ex) {
                        customerId = (long) Math.abs(bu.getId().hashCode());
                    }
                }

                Customer c = Customer.builder()
                        .id(customerId)
                        .name(bu.getName() != null && !bu.getName().isBlank() ? bu.getName() : "Buyer " + cleanPhone)
                        .email(bu.getEmail() != null ? bu.getEmail() : "")
                        .phone(!cleanPhone.isEmpty() ? cleanPhone : bu.getPhoneNumber())
                        .companyName("KFPCL Buyer")
                        .country("India")
                        .status(bu.isEnabled() ? "ACTIVE" : "INACTIVE")
                        .createdAt(bu.getCreatedAt() != null ? bu.getCreatedAt() : java.time.LocalDateTime.now())
                        .build();
                result.add(c);
                if (!cleanPhone.isEmpty()) existingPhones.add(cleanPhone);
                if (!buEmail.isEmpty()) existingEmails.add(buEmail);
            }
        } catch (Exception e) {
            log.warn("Notice while querying buyer_users: {}", e.getMessage());
        }

        // Apply search filter if present
        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            result = result.stream()
                    .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(query))
                            || (c.getPhone() != null && c.getPhone().contains(query))
                            || (c.getEmail() != null && c.getEmail().toLowerCase().contains(query))
                            || (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        // Apply status filter if present
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            String targetStatus = status.trim().toUpperCase();
            result = result.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase(targetStatus))
                    .collect(Collectors.toList());
        }

        // If no pagination requested, return raw array (100% backward compatible)
        if (page == null && size == null) {
            return ResponseEntity.ok(result);
        }

        // Apply pagination
        int p = page != null ? Math.max(0, page) : 0;
        int s = size != null && size > 0 ? size : 10;
        int totalElements = result.size();
        int totalPages = (int) Math.ceil((double) totalElements / s);
        int fromIndex = Math.min(p * s, totalElements);
        int toIndex = Math.min(fromIndex + s, totalElements);
        List<Customer> paginatedList = result.subList(fromIndex, toIndex);

        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedList);
        response.put("customers", paginatedList);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", p);
        response.put("pageSize", s);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Optional<Customer> cOpt = customerRepository.findById(id);
        if (cOpt.isPresent()) {
            return ResponseEntity.ok(cOpt.get());
        }
        return userRepository.findById(id)
                .map(u -> Customer.builder()
                        .id(u.getId())
                        .name(u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Buyer " + u.getPhoneNumber())
                        .email(u.getEmail() != null ? u.getEmail() : "")
                        .phone(u.getPhoneNumber())
                        .companyName(u.getCompanyName() != null ? u.getCompanyName() : "KFPCL Buyer")
                        .country(u.getCity() != null && !u.getCity().isBlank() ? u.getCity() + ", " + (u.getState() != null ? u.getState() : "India") : "India")
                        .status(Boolean.TRUE.equals(u.getIsActive()) ? "ACTIVE" : "INACTIVE")
                        .createdAt(u.getCreatedAt())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customerDetails) {
        Optional<Customer> cOpt = customerRepository.findById(id);
        if (cOpt.isPresent()) {
            Customer c = cOpt.get();
            if (customerDetails.getName() != null) c.setName(customerDetails.getName());
            if (customerDetails.getEmail() != null) c.setEmail(customerDetails.getEmail());
            if (customerDetails.getPhone() != null) c.setPhone(customerDetails.getPhone());
            if (customerDetails.getCompanyName() != null) c.setCompanyName(customerDetails.getCompanyName());
            if (customerDetails.getCountry() != null) c.setCountry(customerDetails.getCountry());
            if (customerDetails.getStatus() != null) c.setStatus(customerDetails.getStatus());
            Customer updated = customerRepository.save(c);
            return ResponseEntity.ok(updated);
        }

        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isPresent()) {
            User u = uOpt.get();
            if (customerDetails.getName() != null) u.setFullName(customerDetails.getName());
            if (customerDetails.getEmail() != null) u.setEmail(customerDetails.getEmail());
            if (customerDetails.getPhone() != null) u.setPhoneNumber(customerDetails.getPhone());
            if (customerDetails.getCompanyName() != null) u.setCompanyName(customerDetails.getCompanyName());
            if (customerDetails.getStatus() != null) u.setIsActive("ACTIVE".equalsIgnoreCase(customerDetails.getStatus()));
            User updatedUser = userRepository.save(u);
            return ResponseEntity.ok(Customer.builder()
                    .id(updatedUser.getId())
                    .name(updatedUser.getFullName())
                    .email(updatedUser.getEmail())
                    .phone(updatedUser.getPhoneNumber())
                    .companyName(updatedUser.getCompanyName())
                    .country(updatedUser.getCity() != null ? updatedUser.getCity() + ", " + updatedUser.getState() : "India")
                    .status(Boolean.TRUE.equals(updatedUser.getIsActive()) ? "ACTIVE" : "INACTIVE")
                    .createdAt(updatedUser.getCreatedAt())
                    .build());
        }

        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Customer> updateCustomerStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.getOrDefault("status", "ACTIVE");
        Optional<Customer> cOpt = customerRepository.findById(id);
        if (cOpt.isPresent()) {
            Customer c = cOpt.get();
            c.setStatus(newStatus);
            Customer updated = customerRepository.save(c);
            return ResponseEntity.ok(updated);
        }

        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isPresent()) {
            User u = uOpt.get();
            u.setIsActive("ACTIVE".equalsIgnoreCase(newStatus));
            User updatedUser = userRepository.save(u);
            return ResponseEntity.ok(Customer.builder()
                    .id(updatedUser.getId())
                    .name(updatedUser.getFullName())
                    .email(updatedUser.getEmail())
                    .phone(updatedUser.getPhoneNumber())
                    .companyName(updatedUser.getCompanyName())
                    .country(updatedUser.getCity() != null ? updatedUser.getCity() + ", " + updatedUser.getState() : "India")
                    .status(Boolean.TRUE.equals(updatedUser.getIsActive()) ? "ACTIVE" : "INACTIVE")
                    .createdAt(updatedUser.getCreatedAt())
                    .build());
        }

        return ResponseEntity.notFound().build();
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Customer deleted", "success", true));
        }
        if (userRepository.existsById(id)) {
            String idStr = String.valueOf(id);
            // 1. Delete associated FCM tokens
            try {
                jdbcTemplate.update("DELETE FROM fcm_tokens WHERE user_id = ?", id);
            } catch (Exception e) {
                log.warn("Could not delete fcm_tokens for user {}: {}", id, e.getMessage());
            }

            // 2. Delete associated addresses
            try {
                jdbcTemplate.update("DELETE FROM addresses WHERE user_id = ?", id);
            } catch (Exception e) {
                log.warn("Could not delete addresses for user {}: {}", id, e.getMessage());
            }

            // 3. Delete associated notifications
            try {
                jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ? OR user_id = ?", idStr, id);
            } catch (Exception e) {
                log.warn("Could not delete notifications for user {}: {}", id, e.getMessage());
            }

            // 4. Delete associated wishlists
            try {
                jdbcTemplate.update("DELETE FROM wishlists WHERE buyer_id = ?", id);
            } catch (Exception e) {
                log.warn("Could not delete wishlists for user {}: {}", id, e.getMessage());
            }

            // 5. Delete associated RFQs and responses
            try {
                jdbcTemplate.update("DELETE FROM rfq_responses WHERE rfq_id IN (SELECT id FROM buyer_rfqs WHERE buyer_id = ?)", idStr);
                jdbcTemplate.update("DELETE FROM buyer_rfqs WHERE buyer_id = ?", idStr);
            } catch (Exception e) {
                log.warn("Could not delete RFQs for user {}: {}", id, e.getMessage());
            }

            // 6. Delete from buyer_users if exists
            try {
                jdbcTemplate.update("DELETE FROM buyer_users WHERE id = ?", idStr);
                userRepository.findById(id).ifPresent(u -> {
                    String cleanPhone = u.getPhoneNumber() != null ? u.getPhoneNumber().replaceAll("[^0-9]", "") : "";
                    if (cleanPhone.length() > 10) cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
                    if (!cleanPhone.isEmpty()) {
                        jdbcTemplate.update("DELETE FROM buyer_users WHERE phone_number = ? OR phone_number LIKE ? OR email LIKE ?", cleanPhone, "%" + cleanPhone, "%" + cleanPhone + "%");
                    }
                    if (u.getEmail() != null && !u.getEmail().isBlank()) {
                        jdbcTemplate.update("DELETE FROM buyer_users WHERE email = ?", u.getEmail());
                    }
                });
            } catch (Exception ignored) {}

            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Customer deleted", "success", true));
        }

        // 7. Check if id matches any buyer_users synthetic ID
        try {
            for (com.project.kfpcl_exports.buyer.model.User bu : buyerUserRepository.findAll()) {
                if (bu.getId() != null && Math.abs((long) bu.getId().hashCode()) == id) {
                    buyerUserRepository.deleteById(bu.getId());
                    return ResponseEntity.ok(Map.of("message", "Customer deleted", "success", true));
                }
            }
        } catch (Exception ignored) {}

        return ResponseEntity.notFound().build();
    }
}
