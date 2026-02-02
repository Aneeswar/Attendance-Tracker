package com.deepak.Attendance.controller;

import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get admin profile
     * GET /api/admin/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            Optional<User> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User userData = user.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", userData.getId());
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching admin profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch profile");
                    }});
        }
    }

    /**
     * Update admin profile
     * POST /api/admin/profile/update
     */
    @PostMapping("/profile/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updateData,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            Optional<User> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User userData = user.get();
            String newUsername = updateData.get("username");
            String newEmail = updateData.get("email");
            String currentPassword = updateData.get("currentPassword");
            String newPassword = updateData.get("newPassword");

            // Validate required fields
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Username is required");
                        }});
            }

            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Email is required");
                        }});
            }

            // Check if username already exists (and it's not the same user)
            Optional<User> existingUser = userRepository.findByUsername(newUsername);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Username already exists");
                        }});
            }

            userData.setUsername(newUsername);
            userData.setEmail(newEmail);

            // If password change is requested, update it
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new HashMap<String, String>() {{
                                put("error", "Current password is required to change password");
                            }});
                }

                // Verify current password
                if (!passwordEncoder.matches(currentPassword, userData.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(new HashMap<String, String>() {{
                                put("error", "Current password is incorrect");
                            }});
                }

                // Encode and update the password
                userData.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(userData);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating admin profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to update profile: " + e.getMessage());
                    }});
        }
    }

    /**
     * Extract user ID from JWT token in Authorization header
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtTokenProvider.extractUsername(token);
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                return user.get().getId();
            }
            throw new IllegalArgumentException("User not found");
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}
