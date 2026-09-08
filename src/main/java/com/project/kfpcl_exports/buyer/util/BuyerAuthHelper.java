package com.project.kfpcl_exports.buyer.util;

import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * No-JWT auth helper.
 * Identity is resolved via plain headers:
 *   X-User-Email   — preferred
 *   X-Phone-Number — secondary
 *   X-Customer-Id  — numeric ID fallback
 * If none are present a default test buyer is returned.
 */
@Component
public class BuyerAuthHelper {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BuyerAuthHelper(@Qualifier("buyerUserRepository") UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Resolves the authenticated Buyer from plain request headers (no JWT).
     */
    public User resolveAuthenticatedBuyer(UserDetails userDetails, HttpServletRequest request) {
        // 0. Check SecurityContextHolder for authenticated UserPrincipal (JWT authentication)
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.project.kfpcl_exports.security.UserPrincipal principal) {
                if (principal.getPhoneNumber() != null && !principal.getPhoneNumber().isBlank()) {
                    return resolveFromIdentifier(principal.getPhoneNumber());
                }
                if (principal.getUserId() != null) {
                    return resolveFromIdentifier(String.valueOf(principal.getUserId()));
                }
            }
        } catch (Exception ignored) {}

        if (request != null) {
            // 1. X-User-Email header
            String email = request.getHeader("X-User-Email");
            if (email != null && !email.isBlank()) {
                return resolveFromIdentifier(email.trim());
            }

            // 2. X-Phone-Number header
            String phone = request.getHeader("X-Phone-Number");
            if (phone != null && !phone.isBlank()) {
                return resolveFromIdentifier(phone.trim());
            }

            // 3. X-Customer-Id header
            String customerId = request.getHeader("X-Customer-Id");
            if (customerId != null && !customerId.isBlank()) {
                return resolveFromIdentifier(customerId.trim());
            }

            // 4. Query param fallbacks for easy testing
            String emailParam = request.getParameter("email");
            if (emailParam != null && !emailParam.isBlank()) {
                return resolveFromIdentifier(emailParam.trim());
            }

            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                return resolveFromIdentifier(tokenParam.trim());
            }
        }

        // 5. Default test buyer fallback
        return getOrCreateDefaultBuyer();
    }

    /**
     * Resolves the authenticated Admin from plain request headers (no JWT).
     */
    public User resolveAuthenticatedAdmin(UserDetails userDetails, HttpServletRequest request) {
        return userRepository.findByEmail("admin@kfpcl.com").orElseGet(() -> {
            User admin = new User();
            admin.setEmail("admin@kfpcl.com");
            admin.setName("KFPCL Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            return userRepository.save(admin);
        });
    }

    /**
     * Resolves or auto-creates a User from email, phone number, or numeric ID.
     */
    public User resolveFromIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || identifier.equalsIgnoreCase("anonymousUser")) {
            return getOrCreateDefaultBuyer();
        }

        // Extract clean 10 digits if identifier contains phone numbers
        String digits = identifier.replaceAll("[^0-9]", "");
        String clean10 = digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;

        // By Phone Number if clean10 is 10 digits
        if (clean10.length() == 10) {
            Optional<User> byPhone = userRepository.findByPhoneNumber(clean10);
            if (byPhone.isPresent()) return byPhone.get();

            Optional<User> byEmailVariant1 = userRepository.findByEmail(clean10 + "@kfpcl-buyer.com");
            if (byEmailVariant1.isPresent()) {
                User u = byEmailVariant1.get();
                if (u.getPhoneNumber() == null) {
                    u.setPhoneNumber(clean10);
                    userRepository.save(u);
                }
                return u;
            }

            Optional<User> byEmailVariant2 = userRepository.findByEmail(clean10 + "@kfpcl.com");
            if (byEmailVariant2.isPresent()) {
                User u = byEmailVariant2.get();
                if (u.getPhoneNumber() == null) {
                    u.setPhoneNumber(clean10);
                    userRepository.save(u);
                }
                return u;
            }

            Optional<User> byPlainPhoneAsEmail = userRepository.findByEmail(clean10);
            if (byPlainPhoneAsEmail.isPresent()) {
                User u = byPlainPhoneAsEmail.get();
                if (u.getPhoneNumber() == null) {
                    u.setPhoneNumber(clean10);
                    userRepository.save(u);
                }
                return u;
            }
        }

        // By Email
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail.get();

        // By Phone Number raw
        Optional<User> byPhone = userRepository.findByPhoneNumber(identifier);
        if (byPhone.isPresent()) return byPhone.get();

        // By ID
        Optional<User> byId = userRepository.findById(identifier);
        if (byId.isPresent()) return byId.get();

        // Auto-create new buyer
        String email = identifier.contains("@")
                ? identifier
                : (clean10.length() == 10 ? clean10 : identifier.replaceAll("[^a-zA-Z0-9]", "")) + "@kfpcl-buyer.com";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(identifier.contains("@") ? identifier.split("@")[0] : identifier);
            if (clean10.length() == 10) {
                newUser.setPhoneNumber(clean10);
            }
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setRole("ROLE_USER");
            newUser.setEnabled(true);
            newUser.setCreatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });
    }

    private User getOrCreateDefaultBuyer() {
        return userRepository.findByEmail("buyer1@kfpcl.com").orElseGet(() -> {
            User buyer = new User();
            buyer.setEmail("buyer1@kfpcl.com");
            buyer.setName("Buyer One");
            buyer.setPassword(passwordEncoder.encode("buyer123"));
            buyer.setRole("ROLE_USER");
            buyer.setEnabled(true);
            buyer.setCreatedAt(LocalDateTime.now());
            return userRepository.save(buyer);
        });
    }
}
