package com.project.kfpcl_exports.controller;

import com.project.kfpcl_exports.dto.AuthDTOs.AppVersionResponse;
import com.project.kfpcl_exports.dto.AuthDTOs.PolicyResponse;
import com.project.kfpcl_exports.service.PolicyAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UtilityController {

    private final PolicyAppService policyAppService;

    @GetMapping("/policies/{type}")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable("type") String type) {
        return ResponseEntity.ok(policyAppService.getPolicy(type));
    }

    @GetMapping("/app/version")
    public ResponseEntity<AppVersionResponse> getAppVersion(
            @RequestParam(value = "platform", defaultValue = "android") String platform) {
        return ResponseEntity.ok(policyAppService.getAppVersion(platform));
    }
}
