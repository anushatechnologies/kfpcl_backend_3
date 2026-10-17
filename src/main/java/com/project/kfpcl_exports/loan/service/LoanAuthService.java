package com.project.kfpcl_exports.loan.service;

import com.project.kfpcl_exports.admin.model.AdminUser;
import com.project.kfpcl_exports.admin.repository.AdminUserRepository;
import com.project.kfpcl_exports.loan.model.LoanApplication;
import com.project.kfpcl_exports.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanAuthService {

    private final AdminUserRepository adminUserRepository;

    public record CurrentIdentity(
            String customerId,
            String phoneNumber,
            String email,
            boolean isAdmin
    ) {}

    /**
     * Resolve identity from SecurityContext or custom headers.
     */
    public CurrentIdentity resolveIdentity(HttpServletRequest request) {
        boolean isAdmin = checkIsAdmin(request);

        String customerId = null;
        String phoneNumber = null;
        String email = null;

        // Check SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (StringUtils.hasText(principal.getBuyerId())) {
                customerId = principal.getBuyerId();
            } else if (principal.getUserId() != null) {
                customerId = String.valueOf(principal.getUserId());
            }
            if (StringUtils.hasText(principal.getPhoneNumber())) {
                phoneNumber = principal.getPhoneNumber();
            }
        }

        // Check HTTP headers
        if (request != null) {
            if (!StringUtils.hasText(customerId)) {
                String hdrCustId = request.getHeader("X-Customer-Id");
                if (StringUtils.hasText(hdrCustId)) customerId = hdrCustId.trim();
            }
            if (!StringUtils.hasText(phoneNumber)) {
                String hdrPhone = request.getHeader("X-Phone-Number");
                if (StringUtils.hasText(hdrPhone)) phoneNumber = hdrPhone.trim();
            }
            if (!StringUtils.hasText(email)) {
                String hdrEmail = request.getHeader("X-User-Email");
                if (StringUtils.hasText(hdrEmail)) email = hdrEmail.trim();
            }
        }

        return new CurrentIdentity(customerId, phoneNumber, email, isAdmin);
    }

    /**
     * Verify if current requester is an authorized KFPCL admin.
     */
    public boolean checkIsAdmin(HttpServletRequest request) {
        if (request == null) return false;

        String adminRoleHeader = request.getHeader("X-Admin-Role");
        if (StringUtils.hasText(adminRoleHeader) && "ADMIN".equalsIgnoreCase(adminRoleHeader.trim())) {
            return true;
        }

        String userRoleHeader = request.getHeader("X-User-Role");
        if (StringUtils.hasText(userRoleHeader) && userRoleHeader.trim().toUpperCase().contains("ADMIN")) {
            return true;
        }

        String adminEmailHeader = request.getHeader("X-Admin-Email");
        if (StringUtils.hasText(adminEmailHeader)) {
            Optional<AdminUser> adminUser = adminUserRepository.findByEmail(adminEmailHeader.trim());
            if (adminUser.isPresent()) {
                return true;
            }
        }

        // Check SecurityContext authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            boolean hasAdminRole = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
            if (hasAdminRole) return true;
        }

        return false;
    }

    /**
     * Verify if the current user can access the given loan application.
     * Admins can access all applications.
     * Customers can access only their own application.
     */
    public boolean canAccessApplication(CurrentIdentity identity, LoanApplication app) {
        if (identity.isAdmin()) {
            return true;
        }

        if (app == null) {
            return false;
        }

        // Check customerId match
        if (StringUtils.hasText(identity.customerId()) && StringUtils.hasText(app.getCustomerId())) {
            if (identity.customerId().equalsIgnoreCase(app.getCustomerId())) {
                return true;
            }
        }

        // Check phoneNumber match (normalize to last 10 digits)
        if (StringUtils.hasText(identity.phoneNumber()) && StringUtils.hasText(app.getPhoneNumber())) {
            String p1 = identity.phoneNumber().replaceAll("[^0-9]", "");
            String p2 = app.getPhoneNumber().replaceAll("[^0-9]", "");
            if (p1.length() > 10) p1 = p1.substring(p1.length() - 10);
            if (p2.length() > 10) p2 = p2.substring(p2.length() - 10);
            if (!p1.isEmpty() && p1.equals(p2)) {
                return true;
            }
        }

        // Check email match
        if (StringUtils.hasText(identity.email()) && StringUtils.hasText(app.getEmail())) {
            if (identity.email().equalsIgnoreCase(app.getEmail().trim())) {
                return true;
            }
        }

        return false;
    }
}
