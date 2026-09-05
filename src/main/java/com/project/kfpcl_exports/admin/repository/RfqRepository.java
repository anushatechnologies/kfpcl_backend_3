package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("adminRfqRepository")
public interface RfqRepository extends JpaRepository<Rfq, Long> {
}
