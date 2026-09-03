package com.project.kfpcl_exports.admin.controller;

import com.project.kfpcl_exports.admin.model.Store;
import com.project.kfpcl_exports.admin.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/stores", "/api/admin/stores"})
@RequiredArgsConstructor
public class StoreController {

    private final StoreRepository storeRepository;

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

    @PostMapping
    public ResponseEntity<Store> createStore(@RequestBody Store store) {
        Store saved = storeRepository.save(store);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Store> updateStore(@PathVariable Long id, @RequestBody Store storeDetails) {
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
            Store updated = storeRepository.save(s);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStore(@PathVariable Long id) {
        if (storeRepository.existsById(id)) {
            storeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Store deleted", "success", true));
        }
        return ResponseEntity.notFound().build();
    }
}
