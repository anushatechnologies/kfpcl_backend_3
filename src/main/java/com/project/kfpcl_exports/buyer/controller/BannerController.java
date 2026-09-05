package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.model.Banner;
import com.project.kfpcl_exports.buyer.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("buyerBannerController")
@RequestMapping("/api/customer/banners")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<Banner>> getBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }
}
