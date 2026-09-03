package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final RfqRepository rfqRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        long totalProducts = productRepository.count();
        long totalCategories = categoryRepository.countByDeletedFalse();
        long totalSubcategories = subcategoryRepository.countByDeletedFalse();
        long totalRfqs = rfqRepository.count();
        long totalCustomers = customerRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalProducts", totalProducts,
                "totalCategories", totalCategories,
                "totalSubcategories", totalSubcategories,
                "totalRfqs", totalRfqs,
                "totalCustomers", totalCustomers,
                "totalRevenue", 154200.0,
                "activeUsers", 1250,
                "success", true
        ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@RequestParam(defaultValue = "today") String period) {
        return ResponseEntity.ok(Map.of(
                "period", period,
                "totalExportOrders", 148,
                "totalQuotationSent", 94,
                "conversionRate", "63.5%",
                "topExportCountry", "United States",
                "salesTrend", List.of(
                        Map.of("label", "09:00", "orders", 12),
                        Map.of("label", "12:00", "orders", 28),
                        Map.of("label", "15:00", "orders", 45),
                        Map.of("label", "18:00", "orders", 32)
                )
        ));
    }

    @GetMapping("/product-performance")
    public ResponseEntity<Map<String, Object>> getProductPerformance(@RequestParam(defaultValue = "today") String period) {
        var trending = productRepository.findByTrendingTrue();
        return ResponseEntity.ok(Map.of(
                "period", period,
                "topProducts", trending,
                "totalViews", 14200,
                "success", true
        ));
    }

    @GetMapping("/active-users")
    public ResponseEntity<Map<String, Object>> getActiveUsers() {
        return ResponseEntity.ok(Map.of(
                "activeBuyers", 342,
                "activeSuppliers", 85,
                "activeDeliveryAgents", 40,
                "totalActive", 467,
                "success", true
        ));
    }
}
