package com.deepak.Attendance.config;

import com.deepak.Attendance.entity.Role;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.RoleRepository;
import com.deepak.Attendance.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role("ADMIN");
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }
        
        Role studentRole = roleRepository.findByName("STUDENT").orElse(null);
        if (studentRole == null) {
            studentRole = new Role("STUDENT");
            roleRepository.save(studentRole);
            log.info("Created STUDENT role");
        }

        // Create default admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setEnabled(true);
            admin.getRoles().add(adminRole);

            userRepository.save(admin);
            log.info("✓ Default admin user created: username=admin, password=admin123");
        }

        log.info("=== Data Initialization Completed ===");
        log.info("Admin Credentials:");
        log.info("  Admin: username=admin, password=admin123");
    }
}
