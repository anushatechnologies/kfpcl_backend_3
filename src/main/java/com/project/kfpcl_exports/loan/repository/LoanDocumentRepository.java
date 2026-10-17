package com.project.kfpcl_exports.loan.repository;

import com.project.kfpcl_exports.loan.model.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanApplicationId(Long loanApplicationId);

    Optional<LoanDocument> findByLoanApplicationIdAndDocumentCategory(Long loanApplicationId, String documentCategory);

    Optional<LoanDocument> findByObjectKey(String objectKey);
}
