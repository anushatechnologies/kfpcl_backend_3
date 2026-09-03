package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
}
