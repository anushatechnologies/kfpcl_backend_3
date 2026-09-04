package com.project.kfpcl_exports.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.kfpcl_exports.admin.model.Store;
import com.project.kfpcl_exports.admin.repository.StoreRepository;
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

@RestController
@RequestMapping({"/api/stores", "/api/admin/stores"})
@RequiredArgsConstructor
public class StoreController {

    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

    private final StoreRepository storeRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Store>> getAllStores() {
        return ResponseEntity.ok(storeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Store> getStoreById(@PathVariable Long id) {
        return storeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Store> createStoreJson(@RequestBody Store store) {
        Store saved = storeRepository.save(store);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> createStoreMultipart(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "store", required = false) String storeJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        try {
            Store store = new Store();

            if (StringUtils.hasText(storeJson)) {
                try {
                    store = objectMapper.readValue(storeJson, Store.class);
                } catch (Exception e) {
                    log.warn("Could not parse store JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) store.setName(name);
            if (StringUtils.hasText(address)) store.setAddress(address);
            if (StringUtils.hasText(city)) store.setCity(city);
            if (StringUtils.hasText(state)) store.setState(state);
            if (StringUtils.hasText(country)) store.setCountry(country);
            if (StringUtils.hasText(phone)) store.setPhone(phone);
            if (StringUtils.hasText(email)) store.setEmail(email);
            if (active != null) store.setActive(active);
            if (StringUtils.hasText(imageUrl)) store.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "stores");
                store.setImageUrl(uploadRes.getUrl());
            }

            if (!StringUtils.hasText(store.getName())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Store name is required", "success", false));
            }

            Store saved = storeRepository.save(store);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to create store: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Store> updateStoreJson(@PathVariable Long id, @RequestBody Store storeDetails) {
        Optional<Store> sOpt = storeRepository.findById(id);
        if (sOpt.isPresent()) {
            Store s = sOpt.get();
            if (storeDetails.getName() != null) s.setName(storeDetails.getName());
            if (storeDetails.getAddress() != null) s.setAddress(storeDetails.getAddress());
            if (storeDetails.getCity() != null) s.setCity(storeDetails.getCity());
            if (storeDetails.getState() != null) s.setState(storeDetails.getState());
            if (storeDetails.getCountry() != null) s.setCountry(storeDetails.getCountry());
            if (storeDetails.getPhone() != null) s.setPhone(storeDetails.getPhone());
            if (storeDetails.getEmail() != null) s.setEmail(storeDetails.getEmail());
            if (storeDetails.getActive() != null) s.setActive(storeDetails.getActive());
            if (storeDetails.getImageUrl() != null) s.setImageUrl(storeDetails.getImageUrl());
            Store updated = storeRepository.save(s);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> updateStoreMultipart(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "store", required = false) String storeJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        Optional<Store> sOpt = storeRepository.findById(id);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Store store = sOpt.get();

            if (StringUtils.hasText(storeJson)) {
                try {
                    Store parsed = objectMapper.readValue(storeJson, Store.class);
                    if (parsed.getName() != null) store.setName(parsed.getName());
                    if (parsed.getAddress() != null) store.setAddress(parsed.getAddress());
                    if (parsed.getCity() != null) store.setCity(parsed.getCity());
                    if (parsed.getState() != null) store.setState(parsed.getState());
                    if (parsed.getCountry() != null) store.setCountry(parsed.getCountry());
                    if (parsed.getPhone() != null) store.setPhone(parsed.getPhone());
                    if (parsed.getEmail() != null) store.setEmail(parsed.getEmail());
                    if (parsed.getActive() != null) store.setActive(parsed.getActive());
                    if (parsed.getImageUrl() != null) store.setImageUrl(parsed.getImageUrl());
                } catch (Exception e) {
                    log.warn("Could not parse store JSON parameter: {}", e.getMessage());
                }
            }

            if (StringUtils.hasText(name)) store.setName(name);
            if (StringUtils.hasText(address)) store.setAddress(address);
            if (StringUtils.hasText(city)) store.setCity(city);
            if (StringUtils.hasText(state)) store.setState(state);
            if (StringUtils.hasText(country)) store.setCountry(country);
            if (StringUtils.hasText(phone)) store.setPhone(phone);
            if (StringUtils.hasText(email)) store.setEmail(email);
            if (active != null) store.setActive(active);
            if (StringUtils.hasText(imageUrl)) store.setImageUrl(imageUrl);

            MultipartFile uploadFile = getFirstNonEmpty(file, image);
            if (uploadFile != null && !uploadFile.isEmpty()) {
                FileUploadResponse uploadRes = s3Service.uploadImage(uploadFile, "stores");
                store.setImageUrl(uploadRes.getUrl());
            } else {
                String textImage = request.getParameter("image");
                if (StringUtils.hasText(textImage)) {
                    store.setImageUrl(textImage);
                }
            }

            Store updated = storeRepository.save(store);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update store: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @PostMapping(value = {"/{id}/image", "/{id}/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStoreImage(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        Optional<Store> sOpt = storeRepository.findById(id);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            MultipartFile targetFile = getFirstNonEmpty(file, image);
            if (targetFile == null || targetFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided", "success", false));
            }
            FileUploadResponse res = s3Service.uploadImage(targetFile, "stores");
            Store store = sOpt.get();
            store.setImageUrl(res.getUrl());
            Store saved = storeRepository.save(store);
            return ResponseEntity.ok(Map.of(
                    "message", "Store image uploaded successfully",
                    "store", saved,
                    "imageUrl", res.getUrl(),
                    "success", true
            ));
        } catch (Exception e) {
            log.error("Failed to upload store image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStore(@PathVariable Long id) {
        if (storeRepository.existsById(id)) {
            storeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Store deleted", "success", true));
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
