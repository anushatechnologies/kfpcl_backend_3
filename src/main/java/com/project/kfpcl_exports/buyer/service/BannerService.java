package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.model.Banner;
import com.project.kfpcl_exports.buyer.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    public List<Banner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }
}
