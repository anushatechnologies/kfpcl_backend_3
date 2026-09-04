package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.RatingRequest;
import com.project.kfpcl_exports.buyer.model.ProductRating;
import com.project.kfpcl_exports.buyer.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("buyerRatingController")
@RequestMapping("/api/customer/products/rating")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping
    public ResponseEntity<?> addRating(@RequestBody RatingRequest request) {
        try {
            ProductRating rating = ratingService.addRating(request);
            return ResponseEntity.ok(rating);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
