package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminProductRepository")
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTitleContainingIgnoreCase(String keyword);
    List<Product> findByTrendingTrue();
    List<Product> findByCategoryId(Long categoryId);
}
