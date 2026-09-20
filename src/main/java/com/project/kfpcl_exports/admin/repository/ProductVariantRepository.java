package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminProductVariantRepository")
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);
    List<ProductVariant> findByProductIdOrderByDisplayOrderAscIdAsc(Long productId);
}
