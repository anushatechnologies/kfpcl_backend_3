package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    Optional<Quotation> findByRfqId(Long rfqId);
}
