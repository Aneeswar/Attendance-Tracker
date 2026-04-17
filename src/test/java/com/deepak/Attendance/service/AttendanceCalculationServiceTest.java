package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.Course;
import com.deepak.Attendance.entity.enums.CourseType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttendanceCalculationServiceTest {

    private final AttendanceCalculationService service = new AttendanceCalculationService();

    @Test
    void calculateRequiredClasses_returnsSafeWhenAlreadyEligible() {
        AttendanceCalculationService.AttendanceCalculationResult result =
                service.calculateRequiredClasses(40, 40, 10);

        assertEquals("SAFE", result.status());
        assertEquals(0, result.minClassesRequired());
    }

    @Test
    void calculateRequiredClasses_returnsAtRiskWhenRecoverable() {
        AttendanceCalculationService.AttendanceCalculationResult result =
                service.calculateRequiredClasses(20, 10, 20);

        assertEquals("AT_RISK", result.status());
        assertEquals(20, result.minClassesRequired());
    }

    @Test
    void calculateRequiredClasses_returnsImpossibleWhenTooLate() {
        AttendanceCalculationService.AttendanceCalculationResult result =
                service.calculateRequiredClasses(30, 5, 5);

        assertEquals("IMPOSSIBLE", result.status());
        assertEquals(6, result.minClassesRequired());
    }

    @Test
    void getAttendanceCutoffDate_forFat_usesPreviousWorkingDay() {
        LocalDate fatDate = LocalDate.of(2026, 5, 2);
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 5, 1));

        LocalDate cutoff = service.getAttendanceCutoffDate(fatDate, "FAT", holidays);

        assertEquals(LocalDate.of(2026, 4, 30), cutoff);
    }

    @Test
    void getAttendanceCutoffDate_forLabFat_usesPreviousSameWeekday() {
        Course labCourse = new Course();
        labCourse.setCourseType(CourseType.LAB);
        LocalDate labFatDate = LocalDate.of(2026, 7, 7); // Tuesday

        LocalDate cutoff = service.getAttendanceCutoffDate(labCourse, labFatDate, "FAT", Set.of());

        assertEquals(LocalDate.of(2026, 6, 30), cutoff); // Previous Tuesday
    }
}
