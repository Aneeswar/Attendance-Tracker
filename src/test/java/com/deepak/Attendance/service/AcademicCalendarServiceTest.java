package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.entity.AcademicCalendar;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarServiceTest {

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private ObjectProvider<StudentService> studentServiceProvider;

    @InjectMocks
    private AcademicCalendarService academicCalendarService;

    @Test
    void saveOrUpdateAcademicCalendar_throwsWhenSemesterAfterExam() {
        AcademicCalendarDTO dto = new AcademicCalendarDTO();
        dto.setSemesterStartDate(LocalDate.of(2026, 5, 1));
        dto.setExamStartDate(LocalDate.of(2026, 4, 1));

        assertThrows(IllegalArgumentException.class, () -> academicCalendarService.saveOrUpdateAcademicCalendar(dto));
    }

    @Test
    void getCurrentAcademicCalendar_returnsEmptyWhenNoRows() {
        when(academicCalendarRepository.findAll()).thenReturn(List.of());

        assertTrue(academicCalendarService.getCurrentAcademicCalendar().isEmpty());
    }

    @Test
    void saveOrUpdateAcademicCalendar_returnsSavedDto() {
        AcademicCalendarDTO dto = new AcademicCalendarDTO();
        dto.setAcademicYear("2026-27");
        dto.setSemesterStartDate(LocalDate.of(2026, 1, 10));
        dto.setExamStartDate(LocalDate.of(2026, 5, 10));

        AcademicCalendar saved = new AcademicCalendar();
        saved.setId(10L);
        saved.setAcademicYear(dto.getAcademicYear());
        saved.setSemesterStartDate(dto.getSemesterStartDate());
        saved.setExamStartDate(dto.getExamStartDate());
        saved.setCreatedAt(LocalDate.now());
        saved.setUpdatedAt(LocalDate.now());

        when(academicCalendarRepository.save(any(AcademicCalendar.class))).thenReturn(saved);

        AcademicCalendarDTO result = academicCalendarService.saveOrUpdateAcademicCalendar(dto);

        verify(academicCalendarRepository).deleteAll();
        assertEquals(10L, result.getId());
        assertEquals("2026-27", result.getAcademicYear());
    }
}
