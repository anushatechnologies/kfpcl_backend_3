package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
}
