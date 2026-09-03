package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("buyerCategoryRepository")
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
    List<Category> findByIsActiveTrue();
}

