package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.StudentHolidayRequestDTO;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.StudentHolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentHolidayService studentHolidayService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthUserResolver authUserResolver;

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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            // Manual token extraction to get username before update
            String token = authHeader.substring(7);
            String currentUsernameFromToken = jwtTokenProvider.extractUsername(token);
            
            Optional<User> user = userRepository.findByUsername(currentUsernameFromToken);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User userData = user.get();
            Long userId = userData.getId();
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

            // Check if email already exists (and it's not the same user)
            Optional<User> existingEmailUser = userRepository.findByEmail(newEmail);
            if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Email already exists");
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

            // Generate new token if username was changed
            String newToken = null;
            if (!currentUsernameFromToken.equals(userData.getUsername())) {
                newToken = jwtTokenProvider.generateTokenForUsername(userData.getUsername());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());
            if (newToken != null) {
                response.put("token", newToken);
            }

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
     * Get all student holiday requests for admin
     * GET /api/admin/holiday-requests
     */
    @GetMapping("/holiday-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllHolidayRequests() {
        try {
            List<StudentHolidayRequestDTO> requests = studentHolidayService.getAllRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error fetching all holiday requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch holiday requests");
                    }});
        }
    }

    /**
     * Approve a holiday request
     * POST /api/admin/holiday-requests/{requestId}/approve
     */
    @PostMapping("/holiday-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveHolidayRequest(@PathVariable Long requestId,
                                                  @RequestBody Map<String, String> body) {
        try {
            String adminComment = body.getOrDefault("comment", "Approved by Admin");
            studentHolidayService.approveRequest(requestId, adminComment);
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Request approved successfully");
            }});
        } catch (Exception e) {
            log.error("Error approving holiday request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Reject a holiday request
     * POST /api/admin/holiday-requests/{requestId}/reject
     */
    @PostMapping("/holiday-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectHolidayRequest(@PathVariable Long requestId,
                                                 @RequestBody Map<String, String> body) {
        try {
            String adminComment = body.getOrDefault("comment", "Rejected by Admin");
            studentHolidayService.rejectRequest(requestId, adminComment);
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Request rejected successfully");
            }});
        } catch (Exception e) {
            log.error("Error rejecting holiday request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

}
