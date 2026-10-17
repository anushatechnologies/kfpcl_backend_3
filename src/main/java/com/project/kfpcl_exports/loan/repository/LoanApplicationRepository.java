package com.project.kfpcl_exports.loan.repository;

import com.project.kfpcl_exports.loan.model.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    List<LoanApplication> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<LoanApplication> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    List<LoanApplication> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT l FROM LoanApplication l WHERE " +
           "(:status IS NULL OR LOWER(l.status) = LOWER(:status)) AND " +
           "(:search IS NULL OR " +
           " LOWER(l.applicantName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LoanApplication> findAllFiltered(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
