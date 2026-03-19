package com.deepak.Attendance.controller;

import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.AttendanceReportRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStudentManagementControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AttendanceReportRepository attendanceReportRepository;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private AdminStudentManagementController adminStudentManagementController;

    @Test
    void searchStudents_returnsOk() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student1");
        user.setEmail("student1@example.com");

        when(userRepository.findByRoleAndSearchTerm("STUDENT", "stud")).thenReturn(List.of(user));
        when(courseRepository.countByUserId(1L)).thenReturn(2L);

        ResponseEntity<?> response = adminStudentManagementController.searchStudents("stud");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
