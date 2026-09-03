package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByType(String type);
}
