package com.project.kfpcl_exports.buyer.controller;

import com.project.kfpcl_exports.buyer.dto.RegistrationRequestDto;
import com.project.kfpcl_exports.buyer.dto.RegistrationResponseDto;
import com.project.kfpcl_exports.buyer.service.BuyerRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1", "/api/auth"})
@RequiredArgsConstructor
public class BuyerRegistrationController {

    private final BuyerRegistrationService registrationService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> registerBuyer(
            @Valid @ModelAttribute RegistrationRequestDto request
    ) {
        RegistrationResponseDto response = registrationService.registerBuyer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Buyer registered successfully in buyer_users. Verification is pending.",
                "data", response
        ));
    }
}
