package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.AttendanceInputRequest;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.StudentHolidayService;
import com.deepak.Attendance.service.StudentService;
import com.deepak.Attendance.repository.UserRepository;
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
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @Mock
    private StudentHolidayService studentHolidayService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUserResolver authUserResolver;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentController studentController;

    @Test
    void submitAttendance_returnsBadRequestWhenCourseCodeMissing() {
        when(authUserResolver.extractUserIdFromToken("Bearer token")).thenReturn(1L);

        AttendanceInputRequest request = new AttendanceInputRequest();
        request.setCourseCode("");
        request.setTotalClassesConducted(10);
        request.setClassesAttended(8);

        ResponseEntity<?> response = studentController.submitAttendance(request, "Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getHolidays_returnsOk() {
        when(studentService.getAllHolidays()).thenReturn(List.of());

        ResponseEntity<?> response = studentController.getHolidays();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
