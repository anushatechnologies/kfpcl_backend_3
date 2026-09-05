package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.model.Wishlist;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import com.project.kfpcl_exports.buyer.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getWishlistProducts(String buyerId) {
        return wishlistRepository.findByBuyerId(buyerId)
                .stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }

    public Wishlist addToWishlist(String buyerId, Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }
        Optional<Wishlist> existing = wishlistRepository.findByBuyerIdAndProductId(buyerId, productId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Wishlist wishlist = new Wishlist(buyerId, productOpt.get());
        return wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(String buyerId, Long productId) {
        wishlistRepository.deleteByBuyerIdAndProductId(buyerId, productId);
    }
}
