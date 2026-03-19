package com.deepak.Attendance.controller;

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
class StudentAcademicCalendarControllerTest {

    @Mock
    private AcademicCalendarService academicCalendarService;

    @InjectMocks
    private StudentAcademicCalendarController studentAcademicCalendarController;

    @Test
    void getAcademicCalendar_returnsOkWithEmptyCalendarObjectWhenNoneConfigured() {
        when(academicCalendarService.getCurrentAcademicCalendar()).thenReturn(Optional.empty());

        ResponseEntity<?> response = studentAcademicCalendarController.getAcademicCalendar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
