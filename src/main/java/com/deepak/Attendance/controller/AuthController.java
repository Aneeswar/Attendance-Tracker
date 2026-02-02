package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.LoginRequest;
import com.deepak.Attendance.dto.LoginResponse;
import com.deepak.Attendance.entity.Role;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.RoleRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                log.error("Invalid login request: username or password is null");
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Username and password are required");
                }});
            }
            
            log.info("Login attempt for username: {}", loginRequest.getUsername());
            
            // Check if user exists
            if (!userRepository.existsByUsername(loginRequest.getUsername())) {
                log.warn("User not found: {}", loginRequest.getUsername());
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Invalid username or password");
                }});
            }
            
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(),
                                loginRequest.getPassword()
                        )
                );

                org.springframework.security.core.userdetails.User userDetails =
                        (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

                String jwt = jwtTokenProvider.generateToken(userDetails);
                
                log.info("Login successful for username: {}", loginRequest.getUsername());

                return ResponseEntity.ok(new LoginResponse(jwt, userDetails.getUsername(), "Login successful"));
            } catch (Exception authEx) {
                log.error("Authentication failed for username: {}, Error: {}", loginRequest.getUsername(), authEx.getMessage());
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Invalid username or password");
                }});
            }
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new HashMap<String, String>() {{
                put("error", "Login failed: " + e.getMessage());
            }});
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest registerRequest) {
        try {
            log.info("Registration attempt for username: {}", registerRequest.getUsername());
            
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                log.warn("Username already exists: {}", registerRequest.getUsername());
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Username already exists");
                }});
            }

            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEmail(registerRequest.getUsername() + "@example.com");
            user.setEnabled(true);

            // Assign STUDENT role by default
            Role studentRole = roleRepository.findByName("STUDENT")
                    .orElseGet(() -> {
                        log.info("Creating STUDENT role");
                        return roleRepository.save(new Role("STUDENT"));
                    });
            user.getRoles().add(studentRole);

            userRepository.save(user);
            log.info("User registered successfully: {}", registerRequest.getUsername());
            
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "User registered successfully");
                put("username", registerRequest.getUsername());
            }});
        } catch (Exception e) {
            log.error("Registration failed for username: {}, Error: {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                put("error", "Registration failed: " + e.getMessage());
            }});
        }
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody LoginRequest registerRequest) {
        return ResponseEntity.status(403).body("Admin registration is disabled. Contact system administrator.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(new LoginResponse("", "", "Logout successful"));
    }
}
