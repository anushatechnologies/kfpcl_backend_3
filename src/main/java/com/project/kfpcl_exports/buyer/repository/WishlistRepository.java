package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByBuyerId(String buyerId);
    Optional<Wishlist> findByBuyerIdAndProductId(String buyerId, Long productId);
    void deleteByBuyerIdAndProductId(String buyerId, Long productId);
}
