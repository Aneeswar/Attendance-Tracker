package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.HolidayDTO;
import com.deepak.Attendance.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayControllerTest {

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private HolidayController holidayController;

    @Test
    void addHoliday_returnsOk() {
        HolidayDTO dto = new HolidayDTO();
        dto.setDate(LocalDate.of(2026, 3, 21));
        dto.setReason("Holiday");

        when(holidayService.addHoliday(dto)).thenReturn(dto);

        ResponseEntity<?> response = holidayController.addHoliday(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void addHoliday_returnsBadRequestWhenServiceFails() {
        HolidayDTO dto = new HolidayDTO();
        when(holidayService.addHoliday(dto)).thenThrow(new IllegalArgumentException("Duplicate"));

        ResponseEntity<?> response = holidayController.addHoliday(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
