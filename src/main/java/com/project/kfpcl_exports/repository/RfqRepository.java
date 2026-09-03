package com.project.kfpcl_exports.repository;

import com.project.kfpcl_exports.model.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Long> {
}
