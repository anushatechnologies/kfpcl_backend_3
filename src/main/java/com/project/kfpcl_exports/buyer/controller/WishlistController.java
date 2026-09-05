package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.WishlistRequest;
import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.model.Wishlist;
import com.project.kfpcl_exports.buyer.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/products/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<Product>> getWishlist(@RequestParam(name = "buyerId", defaultValue = "1") String buyerId) {
        return ResponseEntity.ok(wishlistService.getWishlistProducts(buyerId));
    }

    @PostMapping
    public ResponseEntity<?> addToWishlist(@RequestBody WishlistRequest request) {
        try {
            String buyerId = request.getBuyerId() != null ? request.getBuyerId() : "1";
            Wishlist wishlist = wishlistService.addToWishlist(buyerId, request.getProductId());
            return ResponseEntity.ok(wishlist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> removeFromWishlist(
            @RequestParam(name = "buyerId", defaultValue = "1") String buyerId,
            @RequestParam(name = "productId") Long productId) {
        wishlistService.removeFromWishlist(buyerId, productId);
        return ResponseEntity.ok("Product removed from wishlist");
    }
}
