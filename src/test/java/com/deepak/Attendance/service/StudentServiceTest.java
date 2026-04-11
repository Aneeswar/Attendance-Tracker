package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.TimetableConfirmRequest;
import com.deepak.Attendance.dto.WeeklyScheduleItemDTO;
import com.deepak.Attendance.entity.AttendanceReport;
import com.deepak.Attendance.entity.DateBasedAttendance;
import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.CourseCatalog;
import com.deepak.Attendance.entity.TimetableEntry;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
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

    @Test
    void confirmTimetableWithDates_recalculatesAndSavesReportOnCourseEdit() {
        Long userId = 1L;
        CourseCatalog catalog = new CourseCatalog(10L, "CS101", "Algorithms");
        Course existingCourse = new Course();
        existingCourse.setId(100L);
        existingCourse.setUserId(userId);
        existingCourse.setCourseCode("CS101");
        existingCourse.setCourseName("Algorithms");
        existingCourse.setCourseCatalog(catalog);

        TimetableConfirmRequest req = new TimetableConfirmRequest();
        req.setCourseCode("CS101");
        req.setCourseName("Algorithms");
        req.setSlot("A1");
        req.setCourseStartDate(LocalDate.of(2026, 1, 10));
        req.setWeeklySchedule(List.of(new WeeklyScheduleItemDTO("MONDAY", TimetableEntry.Session.MORNING, 1)));

        when(courseCatalogRepository.findByCourseCodeAndCourseName("CS101", "Algorithms"))
            .thenReturn(Optional.of(catalog));
        when(courseRepository.findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, "CS101"))
            .thenReturn(Optional.of(existingCourse));
        when(courseRepository.save(existingCourse)).thenReturn(existingCourse);
        when(timetableEntryRepository.findByCourseId(100L)).thenReturn(List.of());

        when(academicCalendarService.getCurrentAcademicCalendar()).thenReturn(Optional.empty());
        when(dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(anyLong(), any(), any()))
            .thenReturn(List.of());
        when(attendanceInputRepository.findByCourseId(100L)).thenReturn(Optional.empty());
        when(attendanceCalculationService.calculateFutureClassesAvailable(existingCourse)).thenReturn(0);
        when(attendanceCalculationService.calculateRequiredClasses(0, 0, 0))
            .thenReturn(new AttendanceCalculationService.AttendanceCalculationResult(0.0, 0, 0, "SAFE"));
        when(attendanceReportRepository.findByCourseId(100L)).thenReturn(Optional.empty());
        when(attendanceReportRepository.save(any(AttendanceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.confirmTimetableWithDates(userId, List.of(req));

        verify(attendanceReportRepository).save(any(AttendanceReport.class));
    }

    @Test
    void recalculateAttendanceReportForCourse_usesDateWiseAttendanceTotalsForOverall() {
        Long courseId = 200L;
        Course course = new Course();
        course.setId(courseId);
        course.setUserId(1L);
        course.setCourseCode("LIB2019");
        course.setCourseName("Water and society");
        course.setCourseStartDate(LocalDate.of(2026, 1, 1));

        List<DateBasedAttendance> records = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            DateBasedAttendance dba = new DateBasedAttendance();
            dba.setCourse(course);
            dba.setAttendanceDate(LocalDate.of(2026, 1, 1).plusDays(i));
            dba.setAttended(i < 30);
            records.add(dba);
        }

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(academicCalendarService.getCurrentAcademicCalendar()).thenReturn(Optional.empty());
        when(dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(anyLong(), any(), any()))
                .thenReturn(records);
        when(attendanceCalculationService.calculateFutureClassesAvailable(course)).thenReturn(0);
        when(attendanceCalculationService.calculateRequiredClasses(35, 30, 0))
                .thenReturn(new AttendanceCalculationService.AttendanceCalculationResult(85.71, 0, 0, "SAFE"));
        when(attendanceReportRepository.findByCourseId(courseId)).thenReturn(Optional.empty());
        when(attendanceReportRepository.save(any(AttendanceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.recalculateAttendanceReportForCourse(courseId);

        ArgumentCaptor<AttendanceReport> reportCaptor = ArgumentCaptor.forClass(AttendanceReport.class);
        verify(attendanceReportRepository).save(reportCaptor.capture());
        AttendanceReport saved = reportCaptor.getValue();
        assertEquals(35, saved.getTotalClassesConducted());
        assertEquals(30, saved.getClassesAttended());
    }
}
