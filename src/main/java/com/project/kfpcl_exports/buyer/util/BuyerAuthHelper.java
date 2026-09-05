package com.project.kfpcl_exports.buyer.util;

import com.project.kfpcl_exports.buyer.model.User;
import com.project.kfpcl_exports.buyer.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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

    public BuyerAuthHelper(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Resolves the authenticated Buyer from plain request headers (no JWT).
     */
    public User resolveAuthenticatedBuyer(UserDetails userDetails, HttpServletRequest request) {
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

        // By Email
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail.get();

        // By Phone Number
        Optional<User> byPhone = userRepository.findByPhoneNumber(identifier);
        if (byPhone.isPresent()) return byPhone.get();

        // By ID
        Optional<User> byId = userRepository.findById(identifier);
        if (byId.isPresent()) return byId.get();

        // Auto-create new buyer
        String email = identifier.contains("@")
                ? identifier
                : identifier.replaceAll("[^a-zA-Z0-9]", "") + "@kfpcl-buyer.com";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(identifier.contains("@") ? identifier.split("@")[0] : identifier);
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
