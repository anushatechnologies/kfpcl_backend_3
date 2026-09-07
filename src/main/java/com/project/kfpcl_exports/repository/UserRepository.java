package com.project.kfpcl_exports.repository;

import com.project.kfpcl_exports.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("mainUserRepository")
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByPhoneNumberAndIsActiveTrue(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIsActiveTrue(String phoneNumber);
}
