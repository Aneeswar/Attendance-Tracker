package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.HolidayDTO;
import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private ObjectProvider<StudentService> studentServiceProvider;

    @InjectMocks
    private HolidayService holidayService;

    @Test
    void addHoliday_throwsWhenDuplicateDate() {
        HolidayDTO dto = new HolidayDTO();
        dto.setDate(LocalDate.of(2026, 4, 2));
        dto.setReason("Holiday");

        when(holidayRepository.findByDate(dto.getDate())).thenReturn(Optional.of(new Holiday()));

        assertThrows(IllegalArgumentException.class, () -> holidayService.addHoliday(dto));
    }

    @Test
    void deleteHolidays_doesNothingForEmptyInput() {
        holidayService.deleteHolidays(List.of());

        verify(holidayRepository, never()).deleteAllById(List.of());
    }
}
