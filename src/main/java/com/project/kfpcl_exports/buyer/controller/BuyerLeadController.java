package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.ContactSellerRequest;
import com.project.kfpcl_exports.buyer.model.ContactLead;
import com.project.kfpcl_exports.buyer.service.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buyer")
@CrossOrigin(origins = "*")
public class BuyerLeadController {

    @Autowired
    private LeadService leadService;

    @PostMapping("/contact-seller")
    public ResponseEntity<?> contactSeller(@RequestBody ContactSellerRequest request) {
        try {
            ContactLead lead = leadService.recordLead(request);
            return ResponseEntity.ok(lead);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
