package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminCategoryRepository")
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalse();
    long countByDeletedFalse();
    List<Category> findByNameContainingIgnoreCaseAndDeletedFalse(String name);
    List<Category> findByDiscountGreaterThanEqualAndDeletedFalse(Double minDiscount);
}
