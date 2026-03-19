package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.DateBasedAttendanceDTO;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.AttendanceInputRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.DateBasedAttendanceRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.TimetableEntryRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateBasedAttendanceServiceTest {

    @Mock
    private DateBasedAttendanceRepository dateBasedAttendanceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AttendanceInputRepository attendanceInputRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private ObjectProvider<StudentService> studentServiceProvider;

    @InjectMocks
    private DateBasedAttendanceService dateBasedAttendanceService;

    @Test
    void getCourseAttendanceCalendar_throwsWhenCourseMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dateBasedAttendanceService.getCourseAttendanceCalendar(99L));
    }

    @Test
    void updateDateBasedAttendance_throwsWhenCourseMissing() {
        when(courseRepository.findById(44L)).thenReturn(Optional.empty());

        DateBasedAttendanceDTO dto = new DateBasedAttendanceDTO();
        dto.setDate(LocalDate.now());
        dto.setAttended(true);

        assertThrows(RuntimeException.class,
                () -> dateBasedAttendanceService.updateDateBasedAttendance(44L, List.of(dto)));
    }
}
