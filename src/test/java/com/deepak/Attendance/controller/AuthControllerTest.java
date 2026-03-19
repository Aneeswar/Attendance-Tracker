package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.LoginRequest;
import com.deepak.Attendance.repository.RoleRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_returnsBadRequestWhenUsernameExists() {
        LoginRequest request = new LoginRequest("student1", "pass");
        when(userRepository.existsByUsername("student1")).thenReturn(true);

        ResponseEntity<?> response = authController.register(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_returnsBadRequestWhenBodyInvalid() {
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword(null);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void logout_returnsOk() {
        ResponseEntity<?> response = authController.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
