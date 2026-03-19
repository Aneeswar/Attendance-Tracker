package com.deepak.Attendance.service;

import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.AttendanceInputRepository;
import com.deepak.Attendance.repository.AttendanceReportRepository;
import com.deepak.Attendance.repository.CourseCatalogRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.DateBasedAttendanceRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.TimetableEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TimetableEntryRepository timetableEntryRepository;

    @Mock
    private AttendanceInputRepository attendanceInputRepository;

    @Mock
    private DateBasedAttendanceRepository dateBasedAttendanceRepository;

    @Mock
    private AttendanceCalculationService attendanceCalculationService;

    @Mock
    private HolidayService holidayService;

    @Mock
    private AcademicCalendarService academicCalendarService;

    @Mock
    private AcademicCalendarRepository academicCalendarRepository;

    @Mock
    private AttendanceReportRepository attendanceReportRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private CourseCatalogRepository courseCatalogRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void saveAttendanceInput_throwsWhenCourseNotFound() {
        when(courseRepository.findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(1L, "CS101"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> studentService.saveAttendanceInput(1L, "CS101", 10, 8));
    }
}
