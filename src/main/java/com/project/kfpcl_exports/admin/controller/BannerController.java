package com.project.kfpcl_exports.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Banner;
import com.project.kfpcl_exports.admin.repository.BannerRepository;
import com.project.kfpcl_exports.dto.FileUploadResponse;
import com.project.kfpcl_exports.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("adminBannerController")
@RequestMapping({"/api/banners", "/api/admin/banners"})
@RequiredArgsConstructor
public class BannerController {

    private static final Logger log = LoggerFactory.getLogger(BannerController.class);

    private final BannerRepository bannerRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerRepository.findAll());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Banner> createBannerJson(@RequestBody Banner banner) {
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> createBannerMultipart(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "banner", required = false) String bannerJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        try {
            Banner banner = new Banner();

            if (StringUtils.hasText(bannerJson)) {
                try {
                    banner = objectMapper.readValue(bannerJson, Banner.class);
                } catch (Exception e) {
                    log.warn("Could not parse banner JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(title)) banner.setTitle(title);
            if (StringUtils.hasText(targetUrl)) banner.setTargetUrl(targetUrl);
            if (StringUtils.hasText(position)) banner.setPosition(position);
            if (active != null) banner.setActive(active);
            if (StringUtils.hasText(imageUrl)) banner.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "banners");
                banner.setImageUrl(uploadRes.getUrl());
            }

            Banner saved = bannerRepository.save(banner);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create banner: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Banner> updateBannerJson(@PathVariable Long id, @RequestBody Banner bannerDetails) {
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

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> updateBannerMultipart(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "banner", required = false) String bannerJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        Optional<Banner> bOpt = bannerRepository.findById(id);
        if (bOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Banner banner = bOpt.get();

            if (StringUtils.hasText(bannerJson)) {
                try {
                    Banner parsed = objectMapper.readValue(bannerJson, Banner.class);
                    if (parsed.getTitle() != null) banner.setTitle(parsed.getTitle());
                    if (parsed.getTargetUrl() != null) banner.setTargetUrl(parsed.getTargetUrl());
                    if (parsed.getPosition() != null) banner.setPosition(parsed.getPosition());
                    if (parsed.getActive() != null) banner.setActive(parsed.getActive());
                    if (parsed.getImageUrl() != null) banner.setImageUrl(parsed.getImageUrl());
                } catch (Exception e) {
                    log.warn("Could not parse banner JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(title)) banner.setTitle(title);
            if (StringUtils.hasText(targetUrl)) banner.setTargetUrl(targetUrl);
            if (StringUtils.hasText(position)) banner.setPosition(position);
            if (active != null) banner.setActive(active);
            if (StringUtils.hasText(imageUrl)) banner.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "banners");
                banner.setImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    banner.setImageUrl(textImage);
                }
            }

            Banner updated = bannerRepository.save(banner);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update banner: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PostMapping(value = {"/{id}/image", "/{id}/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBannerImage(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        Optional<Banner> bOpt = bannerRepository.findById(id);
        if (bOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided", "success", false));
            }
            FileUploadResponse res = s3Service.uploadImage(targetFile, "banners");
            Banner banner = bOpt.get();
            banner.setImageUrl(res.getUrl());
            Banner saved = bannerRepository.save(banner);
            return ResponseEntity.ok(Map.of(
                    "message", "Banner image uploaded successfully",
                    "banner", saved,
                    "imageUrl", res.getUrl(),
                    "success", true
            ));
        } catch (Exception e) {
            log.error("Failed to upload banner image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBanner(@PathVariable Long id) {
        Optional<Banner> bOpt = bannerRepository.findById(id);
        if (bOpt.isPresent()) {
            Banner banner = bOpt.get();
            if (StringUtils.hasText(banner.getImageUrl())) {
                try {
                    s3Service.deleteObject(banner.getImageUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete banner image from S3: {}", e.getMessage());
                }
            }
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

    private MultipartFile getFirstNonEmpty(MultipartFile... files) {
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                return f;
            }
        }
        return null;
    }
}
