package com.project.kfpcl_exports.repository;

import com.project.kfpcl_exports.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTitleContainingIgnoreCase(String keyword);
    List<Product> findByTrendingTrue();
    List<Product> findByCategoryId(Long categoryId);
}
