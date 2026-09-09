package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("buyerUserRepository")
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByPanNumber(String panNumber);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPanNumberIgnoreCase(String panNumber);
}
