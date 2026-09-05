package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Rfq;
import com.project.kfpcl_exports.buyer.model.RfqResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqResponseRepository extends JpaRepository<RfqResponse, Long> {

    List<RfqResponse> findByRfqOrderByCreatedAtDesc(Rfq rfq);

    Optional<RfqResponse> findTopByRfqOrderByCreatedAtDesc(Rfq rfq);
}
