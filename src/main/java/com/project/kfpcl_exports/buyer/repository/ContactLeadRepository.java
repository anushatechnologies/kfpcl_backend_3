package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.ContactLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactLeadRepository extends JpaRepository<ContactLead, Long> {
    List<ContactLead> findByBuyerId(String buyerId);
}
