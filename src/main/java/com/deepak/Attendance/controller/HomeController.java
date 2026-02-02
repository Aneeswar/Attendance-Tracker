package com.deepak.Attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/admin/holidays")
    public String adminHolidays() {
        return "admin-holidays";
    }

    @GetMapping("/admin/academic-calendar")
    public String adminAcademicCalendar() {
        return "admin-academic-calendar";
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard() {
        return "student-dashboard";
    }

    @GetMapping("/student/holidays")
    public String studentHolidays() {
        return "student-holidays";
    }

    @GetMapping("/student/attendance")
    public String studentAttendance() {
        return "student-attendance";
    }

    @GetMapping("/student-attendance")
    public String studentAttendanceEntry() {
        return "student-attendance";
    }

    @GetMapping("/student-attendance-report")
    public String studentAttendanceReport() {
        return "student-attendance-report";
    }

    @GetMapping("/student-attendance-calendar")
    public String studentAttendanceCalendar() {
        return "student-attendance-calendar";
    }

    @GetMapping("/student-manage-courses")
    public String studentManageCourses() {
        return "student-manage-courses";
    }

    @GetMapping("/student-mark-by-date")
    public String studentMarkByDate() {
        return "student-mark-by-date";
    }

    @GetMapping("/mark-attendance-by-date")
    public String markAttendanceByDate() {
        return "mark-attendance-by-date";
    }
}
