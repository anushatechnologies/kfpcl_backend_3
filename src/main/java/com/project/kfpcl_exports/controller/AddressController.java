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
public class AddressController {

    private final AddressService addressService;
    private final com.project.kfpcl_exports.repository.UserRepository userRepository;
    private final com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository;

    public AddressController(AddressService addressService,
                             @org.springframework.beans.factory.annotation.Qualifier("mainUserRepository") com.project.kfpcl_exports.repository.UserRepository userRepository,
                             @org.springframework.beans.factory.annotation.Qualifier("buyerUserRepository") com.project.kfpcl_exports.buyer.repository.UserRepository buyerUserRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
        this.buyerUserRepository = buyerUserRepository;
    }

    private Long resolveUserId(UserPrincipal principal) {
        if (principal == null) return null;
        if (principal.getUserId() != null) return principal.getUserId();
        if (principal.getPhoneNumber() != null) {
            java.util.Optional<com.project.kfpcl_exports.model.User> uOpt = userRepository.findByPhoneNumber(principal.getPhoneNumber());
            if (uOpt.isPresent()) return uOpt.get().getId();

            if (buyerUserRepository != null) {
                java.util.Optional<com.project.kfpcl_exports.buyer.model.User> bOpt = buyerUserRepository.findByPhoneNumber(principal.getPhoneNumber());
                if (bOpt.isPresent()) {
                    com.project.kfpcl_exports.buyer.model.User bu = bOpt.get();
                    com.project.kfpcl_exports.model.User newUser = com.project.kfpcl_exports.model.User.builder()
                            .phoneNumber(bu.getPhoneNumber())
                            .fullName(bu.getFullName())
                            .email(bu.getEmail())
                            .companyName(bu.getCompanyName() != null ? bu.getCompanyName() : "KFPCL Buyer")
                            .businessType(bu.getBusinessType() != null ? bu.getBusinessType().name() : "WHOLESALER")
                            .state(bu.getState() != null ? bu.getState() : "Telangana")
                            .city(bu.getCity() != null ? bu.getCity() : "Hyderabad")
                            .isVerified(true)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser).getId();
                }
            }
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(addressService.getAddressesForUser(resolveUserId(principal)));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(resolveUserId(principal), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(resolveUserId(principal), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        addressService.deleteAddress(resolveUserId(principal), id);
        return ResponseEntity.ok(GenericResponse.builder()
                .success(true)
                .message("Address deleted successfully")
                .build());
    }
}
