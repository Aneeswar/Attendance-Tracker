package com.deepak.Attendance.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminPageControllerTest {

    private final AdminPageController controller = new AdminPageController();

    @Test
    void dashboardPage_returnsAdminDashboard() {
        assertEquals("admin-dashboard", controller.dashboardPage());
    }

    @Test
    void adminCourses_returnsAdminCourses() {
        assertEquals("admin-courses", controller.adminCourses());
    }
}
