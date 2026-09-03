package com.project.kfpcl_exports.buyer.repository;

import com.project.kfpcl_exports.buyer.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("buyerBannerRepository")
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByIsActiveTrueOrderBySortOrderAsc();
}
