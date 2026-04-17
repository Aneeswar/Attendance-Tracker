package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.CourseAttendanceCalendarDTO;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.DateBasedAttendanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateBasedAttendanceControllerTest {

    @Mock
    private DateBasedAttendanceService dateBasedAttendanceService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private DateBasedAttendanceController dateBasedAttendanceController;

    @Test
    void getAttendanceCalendar_returnsOk() {
        when(jwtTokenProvider.extractUsername("token")).thenReturn("student1");
        when(dateBasedAttendanceService.getCourseAttendanceCalendar(1L)).thenReturn(new CourseAttendanceCalendarDTO());

        ResponseEntity<?> response = dateBasedAttendanceController.getAttendanceCalendar(1L, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateAttendance_returnsBadRequestOnFailure() {
        when(jwtTokenProvider.extractUsername("token")).thenReturn("student1");
        doThrow(new RuntimeException("fail"))
            .when(dateBasedAttendanceService).updateDateBasedAttendance(anyLong(), anyList());

        ResponseEntity<?> response = dateBasedAttendanceController.updateAttendance(1L, List.of(), "Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
