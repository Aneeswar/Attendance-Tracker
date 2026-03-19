package com.deepak.Attendance.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    private final HomeController controller = new HomeController();

    @Test
    void home_returnsIndex() {
        assertEquals("index", controller.home());
    }

    @Test
    void studentDashboard_returnsStudentDashboard() {
        assertEquals("student-dashboard", controller.studentDashboard());
    }
}
