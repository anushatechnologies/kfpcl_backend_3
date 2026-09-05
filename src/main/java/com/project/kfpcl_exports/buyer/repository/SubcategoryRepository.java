package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("buyerSubcategoryRepository")
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);
}
