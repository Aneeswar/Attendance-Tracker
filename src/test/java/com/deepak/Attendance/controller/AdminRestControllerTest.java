package com.deepak.Attendance.controller;

import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.StudentHolidayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRestControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentHolidayService studentHolidayService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthUserResolver authUserResolver;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminRestController adminRestController;

    @Test
    void getAllHolidayRequests_returnsOk() {
        when(studentHolidayService.getAllRequests()).thenReturn(List.of());

        ResponseEntity<?> response = adminRestController.getAllHolidayRequests();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
