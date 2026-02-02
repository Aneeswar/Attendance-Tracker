package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.LoginRequest;
import com.deepak.Attendance.dto.LoginResponse;
import com.deepak.Attendance.entity.Role;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.RoleRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            String jwt = jwtTokenProvider.generateToken(userDetails);

            return ResponseEntity.ok(new LoginResponse(jwt, userDetails.getUsername(), "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LoginResponse("", "", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getUsername() + "@example.com");
        user.setEnabled(true);

        // Assign STUDENT role by default
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("STUDENT")));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
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
