package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import com.project.kfpcl_exports.buyer.model.Rfq;
import com.project.kfpcl_exports.buyer.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("buyerRfqRepository")
public interface RfqRepository extends JpaRepository<Rfq, Long>, JpaSpecificationExecutor<Rfq> {

    List<Rfq> findByBuyerOrderByCreatedAtDesc(User buyer);

    Page<Rfq> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);

    Page<Rfq> findByBuyerAndStatusOrderByCreatedAtDesc(User buyer, RfqStatus status, Pageable pageable);

    Optional<Rfq> findByIdAndBuyer(Long id, User buyer);

    Optional<Rfq> findByRfqCodeAndBuyer(String rfqCode, User buyer);

    Optional<Rfq> findByRfqCode(String rfqCode);

    Page<Rfq> findByStatusOrderByCreatedAtDesc(RfqStatus status, Pageable pageable);

    Page<Rfq> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<Rfq> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM BuyerRfq r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
