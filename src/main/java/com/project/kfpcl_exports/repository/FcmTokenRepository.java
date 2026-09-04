package com.project.kfpcl_exports.repository;

import com.project.kfpcl_exports.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByFcmToken(String fcmToken);

    List<FcmToken> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    void deleteByFcmToken(String fcmToken);
}
