package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportPreviewResponse;
import com.deepak.Attendance.dto.ManualCourseImportRowRequest;
import com.deepak.Attendance.dto.ResolvedScheduleRowDTO;
import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.CourseCatalog;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.entity.enums.CourseType;
import com.deepak.Attendance.entity.enums.DayOfWeek;
import com.deepak.Attendance.entity.enums.SessionType;
import com.deepak.Attendance.repository.CourseCatalogRepository;
import com.deepak.Attendance.repository.CourseRepository;
import com.deepak.Attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualCourseImportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseCatalogRepository courseCatalogRepository;

    @Mock
    private SlotTokenParserService slotTokenParserService;

    @Mock
    private MasterTimetableSlotResolverService slotResolverService;

    @Mock
    private CourseScheduleGenerationService courseScheduleGenerationService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private ManualCourseImportService manualCourseImportService;

    @Test
    void preview_marksInvalidWhenSlotResolutionFails() {
        Long userId = 1L;
        Semester semester = semester(10L, "Sem 5");
        User user = user(userId, semester);

        ManualCourseImportRowRequest row = new ManualCourseImportRowRequest(
                "STS4003", "Enhancing Programming Ability", "BAD1", LocalDate.of(2025, 11, 29), "THEORY"
        );

        MasterTimetableSlotResolverService.ResolutionResult resolution = new MasterTimetableSlotResolverService.ResolutionResult();
        resolution.getErrors().add("Slot token 'BAD1' does not exist in master timetable");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(slotTokenParserService.parseUniqueTokens("BAD1")).thenReturn(List.of("BAD1"));
        when(slotResolverService.resolveTokens(List.of("BAD1"), SessionType.THEORY)).thenReturn(resolution);

        ManualCourseImportPreviewResponse response = manualCourseImportService.preview(userId, new ManualCourseImportPreviewRequest(List.of(row)));

        assertFalse(response.isValid());
        assertEquals(1, response.getRows().size());
        assertFalse(response.getRows().get(0).getErrors().isEmpty());
    }

    @Test
    void confirm_updatesExistingCourseAndSetsStartDate() {
        Long userId = 1L;
        Semester semester = semester(5L, "Sem 3");
        semester.setSemesterStartDate(LocalDate.of(2025, 12, 9));
        User user = user(userId, semester);

        ManualCourseImportRowRequest row = new ManualCourseImportRowRequest(
                "SWE2003", "Requirements Engineering Management Lab", "L33+L34", LocalDate.of(2025, 11, 15), "LAB"
        );

        ResolvedScheduleRowDTO r1 = new ResolvedScheduleRowDTO("L33", DayOfWeek.TUE, SessionType.LAB, LocalTime.of(16, 0), LocalTime.of(16, 50));
        ResolvedScheduleRowDTO r2 = new ResolvedScheduleRowDTO("L34", DayOfWeek.TUE, SessionType.LAB, LocalTime.of(16, 50), LocalTime.of(17, 40));

        MasterTimetableSlotResolverService.ResolutionResult resolution = new MasterTimetableSlotResolverService.ResolutionResult();
        resolution.setRows(List.of(r1, r2));

        Course existing = new Course();
        existing.setId(45L);
        existing.setUserId(userId);
        existing.setSemester(semester);
        existing.setCourseCode("SWE2003");

        CourseCatalog catalog = new CourseCatalog(8L, "SWE2003", "Requirements Engineering Management Lab");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(slotTokenParserService.parseUniqueTokens("L33+L34")).thenReturn(List.of("L33", "L34"));
        when(slotResolverService.resolveTokens(List.of("L33", "L34"), SessionType.LAB)).thenReturn(resolution);
        when(courseCatalogRepository.findByCourseCodeAndCourseName("SWE2003", "Requirements Engineering Management Lab")).thenReturn(Optional.of(catalog));
        when(courseRepository.findByUserIdAndSemesterIdAndCourseCatalog_CourseCodeIgnoreCase(userId, semester.getId(), "SWE2003"))
                .thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        manualCourseImportService.confirm(userId, new ManualCourseImportPreviewRequest(List.of(row)));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertEquals(CourseType.LAB, saved.getCourseType());
        assertEquals(LocalDate.of(2025, 11, 15), saved.getRegisteredDate());
        assertEquals(LocalDate.of(2025, 12, 9), saved.getCourseStartDate());
        assertEquals("L33+L34", saved.getSlot());
        verify(courseScheduleGenerationService).replaceGeneratedSchedules(eq(existing), eq(List.of(r1, r2)));
    }

    @Test
    void preview_computesEffectiveStartDateAsMaxOfSemesterStartAndRegisteredDate() {
        Long userId = 12L;
        Semester semester = semester(2L, "Sem 2");
        semester.setSemesterStartDate(LocalDate.of(2025, 12, 9));
        User user = user(userId, semester);

        ManualCourseImportRowRequest row = new ManualCourseImportRowRequest(
                "CSE3020", "Product Definition and Validation", "F1+TF1", LocalDate.of(2025, 11, 15), "THEORY"
        );

        MasterTimetableSlotResolverService.ResolutionResult resolution = new MasterTimetableSlotResolverService.ResolutionResult();
        resolution.setRows(List.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(slotTokenParserService.parseUniqueTokens("F1+TF1")).thenReturn(List.of("F1", "TF1"));
        when(slotResolverService.resolveTokens(List.of("F1", "TF1"), SessionType.THEORY)).thenReturn(resolution);

        ManualCourseImportPreviewResponse response = manualCourseImportService.preview(userId, new ManualCourseImportPreviewRequest(List.of(row)));

        assertEquals(LocalDate.of(2025, 11, 15), response.getRows().get(0).getRegisteredDate());
        assertEquals(LocalDate.of(2025, 12, 9), response.getRows().get(0).getEffectiveStartDate());
    }

    @Test
    void confirm_throwsWhenNoSelectedSemester() {
        Long userId = 9L;
        User user = user(userId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ManualCourseImportPreviewRequest request = new ManualCourseImportPreviewRequest(List.of(
                new ManualCourseImportRowRequest("CSE3020", "PDV", "A1", LocalDate.now(), "THEORY")
        ));

        assertThrows(IllegalArgumentException.class, () -> manualCourseImportService.confirm(userId, request));
    }

    private Semester semester(Long id, String name) {
        Semester s = new Semester();
        s.setId(id);
        s.setSemesterName(name);
        return s;
    }

    private User user(Long id, Semester semester) {
        User user = new User();
        user.setId(id);
        user.setUsername("student");
        user.setCurrentSemester(semester);
        return user;
    }
}
