package com.project.kfpcl_exports.repository;

import com.project.kfpcl_exports.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByDeletedFalse();
    long countByDeletedFalse();
    List<Subcategory> findByCategoryIdAndDeletedFalse(Long categoryId);
}
