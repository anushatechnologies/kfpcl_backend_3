package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.AddressRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.AddressResponse;
import com.project.kfpcl_exports.dto.AuthDTOs.GenericResponse;
import com.project.kfpcl_exports.security.UserPrincipal;
import com.project.kfpcl_exports.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getAddressesForUser(principal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        addressService.deleteAddress(principal.getUserId(), id);
        return ResponseEntity.ok(GenericResponse.builder()
                .success(true)
                .message("Address deleted successfully")
                .build());
    }
}
