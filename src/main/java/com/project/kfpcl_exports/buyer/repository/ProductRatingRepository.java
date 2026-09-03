package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRating, Long> {
    List<ProductRating> findByProductId(Long productId);
}
