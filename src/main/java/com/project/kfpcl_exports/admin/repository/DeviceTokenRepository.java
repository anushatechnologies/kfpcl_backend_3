package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);
    List<DeviceToken> findByUserType(String userType);
    List<DeviceToken> findByUserTypeIgnoreCase(String userType);
}


