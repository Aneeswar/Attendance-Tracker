package com.deepak.Attendance.controller;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseManagementControllerTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private AdminCourseManagementController adminCourseManagementController;

    @Test
    void deleteCourse_returnsNotFoundWhenCourseMissing() {
        when(courseRepository.findById(123L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminCourseManagementController.deleteCourse(123L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
