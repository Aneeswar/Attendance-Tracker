package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.service.AcademicCalendarService;
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
class AcademicCalendarControllerTest {

    @Mock
    private AcademicCalendarService academicCalendarService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AcademicCalendarController academicCalendarController;

    @Test
    void setAcademicCalendar_returnsBadRequestWhenServiceThrows() {
        AcademicCalendarDTO dto = new AcademicCalendarDTO();
        when(academicCalendarService.saveOrUpdateAcademicCalendar(dto))
                .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> response = academicCalendarController.setAcademicCalendar(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getCurrentAcademicCalendar_returnsEmptyDtoWhenMissing() {
        when(academicCalendarService.getCurrentAcademicCalendar()).thenReturn(Optional.empty());

        ResponseEntity<?> response = academicCalendarController.getCurrentAcademicCalendar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
