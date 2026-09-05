package com.project.kfpcl_exports.admin.repository;

import com.project.kfpcl_exports.admin.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("adminBannerRepository")
public interface BannerRepository extends JpaRepository<Banner, Long> {
}
