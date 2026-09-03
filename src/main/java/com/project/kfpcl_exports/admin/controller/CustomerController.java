package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.model.Customer;
import com.project.kfpcl_exports.admin.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerRepository.findById(id)
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
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Customer> updateCustomerStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Customer> cOpt = customerRepository.findById(id);
        if (cOpt.isPresent()) {
            Customer c = cOpt.get();
            String newStatus = payload.getOrDefault("status", "ACTIVE");
            c.setStatus(newStatus);
            Customer updated = customerRepository.save(c);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Customer deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }
}
