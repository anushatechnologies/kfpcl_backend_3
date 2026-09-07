package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.AppVersionResponse;
import com.project.kfpcl_exports.dto.AuthDTOs.PolicyResponse;
import org.springframework.stereotype.Service;

@Service
public class PolicyAppService {

    public PolicyResponse getPolicy(String type) {
        String normalizedType = type != null ? type.toLowerCase().trim() : "";
        String content;

        switch (normalizedType) {
            case "privacy-policy":
                content = "# Privacy Policy\n\nKFPCL B2B Marketplace protects buyer information and transactional data strictly in accordance with regulations.";
                break;
            case "terms-conditions":
                content = "# Terms and Conditions\n\nBy accessing the KFPCL B2B Marketplace, buyers agree to abide by our wholesale trading policies and terms.";
                break;
            case "trade-rules":
                content = "# Trade Rules\n\nDetailed guidelines on RFQ creation, supplier quotation matching, quality inspections, and payment settlement.";
                break;
            default:
                throw new IllegalArgumentException("Unknown policy type: " + type + ". Supported types: privacy-policy, terms-conditions, trade-rules");
        }

        return PolicyResponse.builder()
                .type(normalizedType)
                .content(content)
                .build();
    }

    public AppVersionResponse getAppVersion(String platform) {
        // Dynamic app version / mandatory update enforcement logic
        return AppVersionResponse.builder()
                .minVersion("1.0.0")
                .latestVersion("1.2.0")
                .forceUpdate(false)
                .build();
    }
}
