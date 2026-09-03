package com.project.kfpcl_exports.repository;


import com.project.kfpcl_exports.model.CheckoutSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckoutSettingsRepository extends JpaRepository<CheckoutSettings, Long> {
}
