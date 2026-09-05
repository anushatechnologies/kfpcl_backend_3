package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminSubcategoryRepository")
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByDeletedFalse();
    long countByDeletedFalse();
    List<Subcategory> findByCategoryIdAndDeletedFalse(Long categoryId);
}
