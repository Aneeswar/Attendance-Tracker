package com.deepak.Attendance.service;

import org.junit.jupiter.api.Test;

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
}
