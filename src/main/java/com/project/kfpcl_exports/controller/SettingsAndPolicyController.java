package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.model.CheckoutSettings;
import com.project.kfpcl_exports.model.Policy;
import com.project.kfpcl_exports.repository.CheckoutSettingsRepository;
import com.project.kfpcl_exports.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettingsAndPolicyController {

    private final CheckoutSettingsRepository checkoutSettingsRepository;
    private final PolicyRepository policyRepository;

    @GetMapping("/admin/checkout-settings")
    public ResponseEntity<CheckoutSettings> getCheckoutSettings() {
        List<CheckoutSettings> list = checkoutSettingsRepository.findAll();
        if (!list.isEmpty()) {
            return ResponseEntity.ok(list.get(0));
        }
        CheckoutSettings defaultSettings = checkoutSettingsRepository.save(CheckoutSettings.builder()
                .minimumOrderAmount(100.0)
                .taxPercentage(5.0)
                .shippingFee(50.0)
                .enableCod(false)
                .enableOnlinePayment(true)
                .currency("USD")
                .build());
        return ResponseEntity.ok(defaultSettings);
    }

    @PutMapping("/admin/checkout-settings")
    public ResponseEntity<CheckoutSettings> updateCheckoutSettings(@RequestBody CheckoutSettings details) {
        List<CheckoutSettings> list = checkoutSettingsRepository.findAll();
        CheckoutSettings settings = list.isEmpty() ? new CheckoutSettings() : list.get(0);
        if (details.getMinimumOrderAmount() != null) settings.setMinimumOrderAmount(details.getMinimumOrderAmount());
        if (details.getTaxPercentage() != null) settings.setTaxPercentage(details.getTaxPercentage());
        if (details.getShippingFee() != null) settings.setShippingFee(details.getShippingFee());
        if (details.getEnableCod() != null) settings.setEnableCod(details.getEnableCod());
        if (details.getEnableOnlinePayment() != null) settings.setEnableOnlinePayment(details.getEnableOnlinePayment());
        if (details.getCurrency() != null) settings.setCurrency(details.getCurrency());

        CheckoutSettings updated = checkoutSettingsRepository.save(settings);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/policies/{type}")
    public ResponseEntity<Policy> getPolicyByType(@PathVariable String type) {
        return policyRepository.findByType(type)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Policy.builder()
                        .type(type)
                        .title(type.substring(0, 1).toUpperCase() + type.substring(1) + " Policy")
                        .content("Default policy content for " + type)
                        .build()));
    }

    @PutMapping("/admin/policies/{type}")
    public ResponseEntity<Policy> updatePolicyByType(@PathVariable String type, @RequestBody Map<String, String> payload) {
        Optional<Policy> pOpt = policyRepository.findByType(type);
        Policy policy = pOpt.orElseGet(() -> Policy.builder().type(type).build());
        if (payload.containsKey("title")) policy.setTitle(payload.get("title"));
        if (payload.containsKey("content")) policy.setContent(payload.get("content"));

        Policy saved = policyRepository.save(policy);
        return ResponseEntity.ok(saved);
    }
}
