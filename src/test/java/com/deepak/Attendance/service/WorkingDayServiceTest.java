package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.AcademicCalendar;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingDayServiceTest {

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private WorkingDayService workingDayService;

    @Test
    void isWorkingDay_returnsFalseWhenNoCalendar() {
        when(academicCalendarRepository.findAll()).thenReturn(List.of());

        assertFalse(workingDayService.isWorkingDay(LocalDate.now()));
    }

    @Test
    void isWorkingDay_returnsFalseOnSunday() {
        AcademicCalendar cal = new AcademicCalendar();
        cal.setSemesterStartDate(LocalDate.of(2026, 1, 1));
        cal.setExamStartDate(LocalDate.of(2026, 4, 30));

        LocalDate sunday = LocalDate.of(2026, 1, 4);

        when(academicCalendarRepository.findAll()).thenReturn(List.of(cal));

        assertFalse(workingDayService.isWorkingDay(sunday));
    }

    @Test
    void isWorkingDay_returnsTrueOnValidTuesday() {
        AcademicCalendar cal = new AcademicCalendar();
        cal.setSemesterStartDate(LocalDate.of(2026, 1, 1));
        cal.setExamStartDate(LocalDate.of(2026, 4, 30));

        LocalDate tuesday = LocalDate.of(2026, 1, 6);

        when(academicCalendarRepository.findAll()).thenReturn(List.of(cal));
        when(holidayRepository.findByDate(tuesday)).thenReturn(Optional.empty());

        assertTrue(workingDayService.isWorkingDay(tuesday));
    }
}
