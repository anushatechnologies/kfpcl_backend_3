package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.model.Banner;
import com.project.kfpcl_exports.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Banner> createBanner(@RequestBody Banner banner) {
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Banner> updateBanner(@PathVariable Long id, @RequestBody Banner bannerDetails) {
        Optional<Banner> bOpt = bannerRepository.findById(id);
        if (bOpt.isPresent()) {
            Banner b = bOpt.get();
            if (bannerDetails.getTitle() != null) b.setTitle(bannerDetails.getTitle());
            if (bannerDetails.getImageUrl() != null) b.setImageUrl(bannerDetails.getImageUrl());
            if (bannerDetails.getTargetUrl() != null) b.setTargetUrl(bannerDetails.getTargetUrl());
            if (bannerDetails.getPosition() != null) b.setPosition(bannerDetails.getPosition());
            if (bannerDetails.getActive() != null) b.setActive(bannerDetails.getActive());
            Banner updated = bannerRepository.save(b);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable Long id) {
        if (bannerRepository.existsById(id)) {
            bannerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Banner deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Banner> updateBannerStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        Optional<Banner> bOpt = bannerRepository.findById(id);
        if (bOpt.isPresent()) {
            Banner b = bOpt.get();
            if (payload.containsKey("active")) {
                b.setActive(payload.get("active"));
            } else if (payload.containsKey("status")) {
                b.setActive(payload.get("status"));
            }
            Banner updated = bannerRepository.save(b);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }
}
