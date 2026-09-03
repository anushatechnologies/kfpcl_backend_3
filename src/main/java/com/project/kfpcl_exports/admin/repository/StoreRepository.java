package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
}
